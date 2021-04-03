package router

import akka.actor.typed.{
  ActorSystem,
  Behavior,
  DispatcherSelector,
  SupervisorStrategy
}
import akka.actor.typed.scaladsl.{Behaviors, Routers}

object PoolRouterExample extends App {

  object Main {
    import Worker._
    def apply(): Behavior[Command] = {
      val router = Routers.pool(poolSize = 4) {
        Behaviors
          .supervise {
            Worker()
          }
          .onFailure(SupervisorStrategy.restart)
      }
      router
        .withRouteeProps(DispatcherSelector.blocking())
        .withRoundRobinRouting()
    }
  }

  val system = ActorSystem(Main(), "system")

  for (x <- 0 to 10) {
    system ! Worker.DoLog(s"message-${x}")
  }

  Thread.sleep(100)
  system.terminate()
}
