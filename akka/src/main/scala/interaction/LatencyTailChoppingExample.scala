package interaction

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.reflect.ClassTag

object LatencyTailChoppingExample extends App {

  object TailChopping {

    sealed trait Command

    private case object RequestTimeout extends Command

    private case object FinalTimeout extends Command

    private final case class WrappedReply[R](reply: R) extends Command

    def apply[Reply: ClassTag](
        sendRequest: (Int, ActorRef[Reply]) => Unit,
        nextRequestAfter: FiniteDuration,
        replyTo: ActorRef[Reply],
        finalTimeout: FiniteDuration,
        timeoutReply: Reply
    ): Behavior[Command] = {
      Behaviors.setup { context =>
        Behaviors.withTimers { timers =>
          val replyAdapter = context.messageAdapter[Reply](WrappedReply(_))

          def waiting(requestCount: Int): Behavior[Command] = {
            Behaviors.logMessages {
              Behaviors.receiveMessage {
                case WrappedReply(reply: Reply) =>
                  replyTo ! reply
                  Behaviors.stopped
                case RequestTimeout =>
                  sendNextRequest(requestCount + 1)
                case FinalTimeout =>
                  replyTo ! timeoutReply
                  Behaviors.stopped
              }
            }
          }

          def sendNextRequest(requestCount: Int): Behavior[Command] = {
            sendRequest(requestCount, replyAdapter)
            timers.startSingleTimer(RequestTimeout, nextRequestAfter)
            waiting(requestCount)
          }

          timers.startSingleTimer(FinalTimeout, finalTimeout)
          sendNextRequest(1)
        }
      }
    }
  }

  object Main {

    private final case class Request(count: Int, replyTo: ActorRef[Response])

    sealed trait Response

    private final case class Success(count: Int) extends Response

    private case object Failure extends Response

    private def serverBehavior(
        processing: FiniteDuration,
        successRatio: Double
    ): Behavior[Request] = {
      Behaviors.logMessages {
        Behaviors.setup[Request] { context =>
          Behaviors.receiveMessage { request =>
            val handler = Behaviors.withTimers[Request] { timers =>
              timers.startSingleTimer(request, processing)
              Behaviors.receiveMessage { message =>
                if (math.random() < successRatio) {
                  message.replyTo ! Success(message.count)
                }
                Behaviors.stopped
              }
            }
            context.spawnAnonymous(handler)
            Behaviors.same
          }
        }
      }
    }

    private def clientBehavior(): Behavior[Response] = {
      Behaviors.logMessages(Behaviors.ignore[Response])
    }

    def apply(): Behavior[NotUsed] = {
      Behaviors.setup { context =>
        val processing: FiniteDuration = 200.millis

        val server: ActorRef[Request] = context.spawn(
          serverBehavior(processing, successRatio = 0.3),
          "server"
        )
        val client: ActorRef[Response] =
          context.spawn(clientBehavior(), "client")

        val timeout: FiniteDuration = 1000.millis
        val interval: FiniteDuration = 100.millis

        def sendRequest(
            requestCount: Int,
            replyTo: ActorRef[Response]
        ): Unit = {
          server ! Request(requestCount, replyTo)
        }

        context.spawn(
          TailChopping(sendRequest, interval, client, timeout, Failure),
          "tail-chopping"
        )
        Behaviors.same
      }
    }
  }

  val system = ActorSystem(Main(), "system")
  Thread.sleep(2000)
  system.terminate()
}
