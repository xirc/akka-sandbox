package fsm

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.concurrent.duration.DurationInt

object FSMExample extends App {
  object Buncher {
    sealed trait Event
    final case class SetTarget(ref: ActorRef[Batch]) extends Event
    final case class Queue(obj: Any) extends Event
    case object Flush extends Event
    private case object Timeout extends Event

    final case class Batch(obj: Seq[Any])

    private sealed trait Data
    private case object Uninitialized extends Data
    private final case class Todo(target: ActorRef[Batch], queue: Seq[Any])
        extends Data

    def apply(): Behavior[Event] = idle(Uninitialized)

    private def idle(data: Data): Behavior[Event] = {
      Behaviors.receiveMessage { message =>
        (message, data) match {
          case (SetTarget(ref), Uninitialized) =>
            idle(Todo(ref, Vector.empty))
          case (Queue(obj), t @ Todo(_, queue)) =>
            active(t.copy(queue = queue :+ obj))
          case _ =>
            Behaviors.unhandled
        }
      }
    }

    private def active(data: Todo): Behavior[Event] = {
      Behaviors.withTimers { timers =>
        timers.startSingleTimer(Timeout, 1.second)
        Behaviors.receiveMessagePartial {
          case Flush | Timeout =>
            data.target ! Batch(data.queue)
            idle(data.copy(queue = Vector.empty))
          case Queue(obj) =>
            active(data.copy(queue = data.queue :+ obj))
        }
      }
    }
  }

  val system = ActorSystem(Buncher(), "system")
  val logger =
    system.systemActorOf(Behaviors.logMessages(Behaviors.ignore[Any]), "logger")

  import Buncher._
  system ! SetTarget(logger)

  for (x <- 1 to 10) {
    system ! Queue(x)
  }
  Thread.sleep(1500)

  for (x <- 11 to 20) {
    system ! Queue(x)
  }
  Thread.sleep(1500)

  system.terminate()
}
