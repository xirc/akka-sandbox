package dispatchers

import akka.NotUsed
import akka.actor.typed.{ActorSystem, Behavior, DispatcherSelector}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.{ExecutionContext, Future}

object BlockingActorExample extends App {
  object PrintActor {
    def apply(): Behavior[Int] = {
      Behaviors.receiveMessage { i =>
        println(s"PrintActor: ${i}")
        Behaviors.same
      }
    }
  }

  object BlockingActor {
    def apply(): Behavior[Int] = {
      Behaviors.setup { context =>
        implicit val ec =
          context.system.dispatchers.lookup(DispatcherSelector.blocking())
        Behaviors.receiveMessage { i =>
          trigger(i)
          Behaviors.same
        }
      }
    }
    private def trigger(
      i: Int
    )(implicit executionContext: ExecutionContext): Future[Unit] = {
      println(s"Calling blocking future: ${i}")
      Future {
        Thread.sleep(1000)
        println(s"Blocking future Finished: ${i}")
      }
    }
  }

  val root = Behaviors.setup[NotUsed] { context =>
    for (i <- 1 to 50) {
      val a = context.spawn(BlockingActor(), s"BlockingActor-${i}")
      val b = context.spawn(PrintActor(), s"PrintActor-${i}")
      a ! i
      b ! i
    }
    Behaviors.empty
  }

  val system = ActorSystem(root, "system")
  Thread.sleep(10000)
  system.terminate()
}
