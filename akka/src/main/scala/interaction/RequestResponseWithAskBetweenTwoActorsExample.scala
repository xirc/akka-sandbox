package interaction

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object RequestResponseWithAskBetweenTwoActorsExample extends App {

  object Hal {

    sealed trait Command

    case class OpenThePodBayDoorsPlease(replyTo: ActorRef[Response]) extends Command

    case class Response(message: String)

    def apply(): Behavior[Command] = {
      Behaviors.receiveMessage[Command] {
        case OpenThePodBayDoorsPlease(replyTo) =>
          replyTo ! Response("I'm sorry, Dave. I'm afraid I can't do that.")
          Behaviors.same
      }
    }
  }

  object Dave {

    sealed trait Command

    private case class AdaptedResponse(message: String) extends Command

    def apply(hal: ActorRef[Hal.Command]): Behavior[Command] = {
      Behaviors.setup[Command] { context =>
        implicit val timeout: Timeout = 3.seconds

        context.ask(hal, Hal.OpenThePodBayDoorsPlease) {
          case Success(Hal.Response(message)) => AdaptedResponse(message)
          case Failure(_) => AdaptedResponse("Request failed")
        }

        val requestId = 1
        context.ask(hal, Hal.OpenThePodBayDoorsPlease) {
          case Success(Hal.Response(message)) => AdaptedResponse(s"$requestId: $message")
          case Failure(_) => AdaptedResponse(s"$requestId: Request failed")
        }

        Behaviors.receiveMessage {
          case AdaptedResponse(message) =>
            context.log.info("Got response from hal: {}", message)
            Behaviors.same
        }
      }
    }
  }

  object Main {
    def apply(): Behavior[NotUsed] = {
      Behaviors.setup { context =>
        val hal = context.spawn(Hal(), "hal")
        context.spawn(Dave(hal), "dave")
        Behaviors.empty
      }
    }
  }

  val system = ActorSystem(Main(), "system")
  Thread.sleep(100)
  system.terminate()
}
