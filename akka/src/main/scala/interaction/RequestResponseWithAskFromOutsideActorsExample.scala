package interaction

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object RequestResponseWithAskFromOutsideActorsExample extends App {

  object CookieFabric {

    sealed trait Command

    case class GiveMeCookies(count: Int, replyTo: ActorRef[Reply]) extends Command

    sealed trait Reply

    case class Cookies(count: Int) extends Reply

    case class InvalidRequest(reason: String) extends Reply

    def apply(): Behavior[Command] = {
      Behaviors.receiveMessage {
        case GiveMeCookies(count, replyTo) =>
          if (count >= 5) {
            replyTo ! InvalidRequest("Too many cookies.")
          } else {
            replyTo ! Cookies(count)
          }
          Behaviors.same
      }
    }
  }

  implicit val timeout: Timeout = 3.seconds
  implicit val system = ActorSystem(CookieFabric(), "system")
  import system.executionContext
  val cookieFabric: ActorRef[CookieFabric.Command] = system

  val result = cookieFabric.ask(CookieFabric.GiveMeCookies(3, _))
  result.onComplete {
    case Success(CookieFabric.Cookies(count)) =>
      println(s"Yay, $count cookies!")
    case Success(CookieFabric.InvalidRequest(reason)) =>
      println(s"No cookies for me. $reason")
    case Failure(exception) =>
      println(s"Boo! didn't get cookies: ${exception.getMessage}")
  }

  Thread.sleep(100)
  system.terminate()
}
