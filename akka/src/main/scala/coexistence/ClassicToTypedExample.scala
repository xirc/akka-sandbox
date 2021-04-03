package coexistence

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.{ actor => classic }

object ClassicToTypedExample extends App {

  object Typed {
    sealed trait Command
    final case class Ping(replyTo: ActorRef[Pong.type]) extends Command
    case object Pong

    def apply(): Behavior[Command] = {
      Behaviors.receive { (context, message) =>
        message match {
          case Ping(replyTo) =>
            context.log.info(s"${context.self} got Ping from $replyTo")
            replyTo ! Pong
            Behaviors.same
        }
      }
    }
  }

  object Classic {
    def props(): classic.Props = classic.Props(new Classic())
  }
  class Classic extends classic.Actor with classic.ActorLogging {
    val second: ActorRef[Typed.Command] =
      context.spawn(Typed(), "second")

    context.watch(second)

    second ! Typed.Ping(self)

    override def receive: Receive = {
      case Typed.Pong =>
        log.info(s"$self got Pong from ${sender()}")
        context.stop(second)
      case classic.Terminated(ref) =>
        log.info(s"$self observed termination of $ref")
        context.stop(self)
    }
  }


  val system: classic.ActorSystem = classic.ActorSystem("class-to-typed-system")
  val typedSystem: ActorSystem[Nothing] = system.toTyped

  val typedOne = system.spawn(Typed(), "typed-one")
  typedOne ! Typed.Ping(typedSystem.ignoreRef)

  val classicOne = system.actorOf(Classic.props(), "classic-one")

  Thread.sleep(1000)
  system.terminate()
}
