package shutdown

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.Future

object AddCancellableTaskExample extends App {
  object Guardian {
    sealed trait Command
    case object Cancel extends Command

    def apply(): Behavior[Command] = {
      Behaviors.setup { context =>
        implicit val sys = context.system
        import sys.executionContext

        val cancellable =
          CoordinatedShutdown(context.system).addCancellableTask(
            CoordinatedShutdown.PhaseBeforeServiceUnbind,
            "cleanup"
          ) { () =>
            Future {
              println("cleanup")
              Done
            }
          }

        Behaviors.receiveMessage { case Cancel =>
          cancellable.cancel()
          Behaviors.same
        }
      }
    }
  }

  object MyShutdownReason extends CoordinatedShutdown.Reason
  val system = ActorSystem(Guardian(), "system")
  system ! Guardian.Cancel
  CoordinatedShutdown(system).runAll(MyShutdownReason)
}
