package shutdown

import akka.NotUsed
import akka.actor.CoordinatedShutdown
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt

object AddTaskExample extends App {
  object Guardian {
    def apply(): Behavior[NotUsed] = {
      Behaviors.setup { context =>
        implicit val sys = context.system
        val myActor = context.spawn(MyActor(), "my-actor")
        CoordinatedShutdown(context.system).addTask(
          CoordinatedShutdown.PhaseBeforeServiceUnbind, "my-task"
        ) { () =>
          implicit val timeout: Timeout = 5.seconds
          myActor.ask(MyActor.Stop)
        }
        Behaviors.ignore
      }
    }
  }

  object MyShutdownReason extends CoordinatedShutdown.Reason
  val system = ActorSystem(Guardian(), "system")
  CoordinatedShutdown(system).runAll(MyShutdownReason)
}
