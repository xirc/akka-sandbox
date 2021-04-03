package coexistence

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.{actor => classic}
import akka.actor.typed.scaladsl.adapter._

object TypedToClassicExample extends App {

  object Classic {
    def props(): classic.Props = classic.Props(new Classic())
  }
  class Classic extends classic.Actor with classic.ActorLogging {
    override def receive: Receive = { case Typed.Ping(replyTo) =>
      log.info(
        "Received Ping from {}, and then Send back to {}",
        sender(),
        replyTo
      )
      replyTo ! Typed.Pong
    }
  }

  object Typed {
    final case class Ping(replyTo: ActorRef[Pong.type])
    sealed trait Command
    case object Pong extends Command

    def apply(): Behavior[Command] = {
      Behaviors.setup { context =>
        val classicOne = context.actorOf(Classic.props(), "second")
        context.watch(classicOne)
        classicOne.tell(Typed.Ping(context.self), context.self.toClassic)
        Behaviors
          .receivePartial[Command] { case (context, Pong) =>
            context.log.info("Received Pong")
            context.stop(classicOne)
            Behaviors.same
          }
          .receiveSignal { case (_, Terminated(ref)) =>
            context.log.info("Received Terminated({})", ref)
            Behaviors.stopped
          }
      }
    }
  }

  val system = classic.ActorSystem("typed-watching-classic")
  val typed = system.spawn(Typed(), "typed-one")
  Thread.sleep(100)
  system.terminate()
}
