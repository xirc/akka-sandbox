package interaction

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.pattern.StatusReply
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object GenericResponseWrapperWithAskFromOutside extends App {

  object CookieFabric {

    sealed trait Command

    final case class GiveMeCookies(
        count: Int,
        replyTo: ActorRef[StatusReply[Cookies]]
    ) extends Command

    final case class Cookies(count: Int)

    def apply(): Behavior[Command] = {
      Behaviors.receiveMessage { case GiveMeCookies(count, replyTo) =>
        if (count >= 5) {
          replyTo ! StatusReply.Error("Too many cookies.")
        } else {
          replyTo ! StatusReply.Success(Cookies(count))
        }
        Behaviors.same
      }
    }
  }

  implicit val timeout: Timeout = 3.seconds
  implicit val system = ActorSystem(CookieFabric(), "system")
  import system.executionContext
  val cookieFabric: ActorRef[CookieFabric.Command] = system

  val result = cookieFabric.askWithStatus(CookieFabric.GiveMeCookies(3, _))
  result.onComplete {
    case Success(CookieFabric.Cookies(count)) =>
      println(s"Yay, $count cookies!")
    case Failure(StatusReply.ErrorMessage(reason)) =>
      println(s"No cookies for me. $reason")
    case Failure(exception) =>
      println(s"Boo! didn't get cookies: ${exception.getMessage}")
  }

  Thread.sleep(100)
  system.terminate()
}
