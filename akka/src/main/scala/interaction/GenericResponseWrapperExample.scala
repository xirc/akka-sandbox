package interaction

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.pattern.StatusReply
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object GenericResponseWrapperExample extends App {

  object Hal {

    sealed trait Command

    case class OpenThePodBayDoorsPlease(replyTo: ActorRef[StatusReply[String]])
        extends Command

    def apply(): Behavior[Command] = {
      Behaviors.receiveMessage { case OpenThePodBayDoorsPlease(replyTo) =>
        replyTo ! StatusReply.Error(
          "I'm sorry, Dave. I'm afraid I can't do that."
        )
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

        context.askWithStatus(hal, Hal.OpenThePodBayDoorsPlease) {
          case Success(message) =>
            AdaptedResponse(message)
          case Failure(StatusReply.ErrorMessage(text)) =>
            AdaptedResponse(s"Request denied: $text")
          case Failure(_) =>
            AdaptedResponse("Request failed")
        }

        Behaviors.receiveMessage { case AdaptedResponse(message) =>
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
        val _ = context.spawn(Dave(hal), "dave")
        Behaviors.empty
      }
    }
  }

  val system = ActorSystem(Main(), "system")
  Thread.sleep(100)
  system.terminate()
}
