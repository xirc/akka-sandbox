package router

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{Behaviors, Routers}

object GroupRouterExample extends App {
  val serviceKey = ServiceKey[Worker.Command]("log-worker")

  object Main {
    import Worker._
    def apply(): Behavior[Command] = {
      Behaviors.setup { context =>
        for (i <- 1 to 4) {
          val worker = context.spawn(Worker(), s"worker-${i}")
          context.system.receptionist ! Receptionist.Register(
            serviceKey,
            worker
          )
        }
        Routers.group(serviceKey)
      }
    }
  }

  val system = ActorSystem(Main(), "system")

  for (i <- 0 to 10) {
    system ! Worker.DoLog(s"message-${i}")
  }

  Thread.sleep(100)
  system.terminate()
}
