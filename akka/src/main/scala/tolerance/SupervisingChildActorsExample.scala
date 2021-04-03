package tolerance

import akka.NotUsed
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors

private object SupervisingChildActorsExample extends App {
  private def child(size: Long = 0): Behavior[String] = {
    Behaviors.logMessages {
      Behaviors.setup { context =>
        context.log.info("size: {}", size)
        Behaviors.receiveMessage { message =>
          child(size + message.length)
        }
      }
    }
  }

  private def parentCore(child1: ActorRef[String], child2: ActorRef[String]): Behavior[String] = {
    Behaviors.receiveMessage[String] { message =>
      val parts = message.split(" ")
      child1 ! parts(0)
      child2 ! parts(1)
      Behaviors.same
    }
  }

  private def parent1: Behavior[String] = {
    Behaviors
      .supervise[String] {
        Behaviors.setup { context =>
          val child1 = context.spawn(child(), "child1")
          val child2 = context.spawn(child(), "child2")
          parentCore(child1, child2)
        }
      }
      .onFailure(SupervisorStrategy.restart)
  }

  private def parent2: Behavior[String] = {
    Behaviors.setup { context =>
      val child1 = context.spawn(child(), "child1")
      val child2 = context.spawn(child(), "child2")
      Behaviors
        .supervise {
          parentCore(child1, child2)
        }
        .onFailure(SupervisorStrategy.restart.withStopChildren(false))
    }
  }

  private def main: Behavior[NotUsed] = {
    Behaviors.setup { context =>
      val p1 = context.spawn(parent1, "parent1")
      val p2 = context.spawn(parent2, "parent2")

      p1 ! "hello world!"
      p2 ! "world hello!"
      p1 ! "???"
      p2 ! "!!!"
      p1 ! "hello world!"
      p2 ! "world hello!"

      Behaviors.same
    }
  }

  val system = ActorSystem(main, "system")
  Thread.sleep(100)
  system.terminate()
}
