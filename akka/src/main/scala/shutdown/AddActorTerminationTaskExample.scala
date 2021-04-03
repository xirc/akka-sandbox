package shutdown

import akka.NotUsed
import akka.actor.CoordinatedShutdown
import akka.actor.typed.scaladsl.adapter.TypedActorRefOps
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

object AddActorTerminationTaskExample extends App {
  object Guardian {
    def apply(): Behavior[NotUsed] = {
      Behaviors.setup { context =>
        val myActor = context.spawn(MyActor(), "my-actor")
        CoordinatedShutdown(context.system).addActorTerminationTask(
          CoordinatedShutdown.PhaseBeforeServiceUnbind,
          "my-task",
          myActor.toClassic,
          Some(MyActor.Stop(context.system.ignoreRef))
        )
        Behaviors.ignore
      }
    }
  }

  object MyShutdownReason extends CoordinatedShutdown.Reason
  val system = ActorSystem(Guardian(), "system")
  CoordinatedShutdown(system).runAll(MyShutdownReason)
}
