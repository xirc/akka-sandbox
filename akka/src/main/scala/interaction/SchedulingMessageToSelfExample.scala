package interaction

import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object SchedulingMessageToSelfExample extends App {

  object Buncher {

    sealed trait Command

    final case class ExcitingMessage(message: String) extends Command

    final case class Batch(messages: Vector[Command])

    private case object Timeout extends Command

    private case object TimerKey

    def apply(
        target: ActorRef[Batch],
        after: FiniteDuration,
        maxSize: Int
    ): Behavior[Command] = {
      Behaviors.withTimers[Command] { timers =>
        new Buncher(timers, target, after, maxSize).idle()
      }
    }
  }

  class Buncher(
      timers: TimerScheduler[Buncher.Command],
      target: ActorRef[Buncher.Batch],
      after: FiniteDuration,
      maxSize: Int
  ) {

    import Buncher._

    private def idle(): Behavior[Command] = {
      Behaviors.receiveMessage { message =>
        timers.startSingleTimer(TimerKey, Timeout, after)
        active(Vector(message))
      }
    }

    private def active(buffer: Vector[Command]): Behavior[Command] = {
      Behaviors.receiveMessage {
        case Timeout =>
          target ! Batch(buffer)
          idle()
        case message if buffer.size + 1 < maxSize =>
          active(buffer :+ message)
        case message =>
          timers.cancel(TimerKey)
          target ! Batch(buffer :+ message)
          idle()
      }
    }
  }

  object BatchExecutor {
    def apply(): Behavior[Buncher.Batch] = {
      Behaviors.logMessages(Behaviors.ignore)
    }
  }

  object Main {
    def apply(
        after: FiniteDuration,
        maxSize: Int
    ): Behavior[Buncher.Command] = {
      Behaviors.setup { context =>
        val batchExecutor = context.spawn(BatchExecutor(), "batch-executor")
        val buncher =
          context.spawn(Buncher(batchExecutor, after, maxSize), "buncher")
        Behaviors.receiveMessage { message =>
          buncher ! message
          Behaviors.same
        }
      }
    }
  }

  val system = ActorSystem(Main(300.millis, 3), "system")

  for (x <- 1 to 9) {
    system ! Buncher.ExcitingMessage(s"message-${x}")
  }

  Thread.sleep(500)

  system ! Buncher.ExcitingMessage("a")
  system ! Buncher.ExcitingMessage("b")

  Thread.sleep(500)

  system ! Buncher.ExcitingMessage("c")
  system ! Buncher.ExcitingMessage("d")

  Thread.sleep(500)

  system.terminate()
}
