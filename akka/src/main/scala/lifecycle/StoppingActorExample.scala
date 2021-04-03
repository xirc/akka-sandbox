package lifecycle

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, PostStop}
import org.slf4j.Logger

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object StoppingActorExample extends App {

  object Master {

    sealed trait Command

    final case class SpawnJob(name: String) extends Command

    case object GracefulShutdown extends Command

    private def cleanup(log: Logger): Unit = {
      log.info("Cleaning up!")
    }

    def apply(): Behavior[Command] = {
      Behaviors
        .receive[Command] { (context, message) =>
          message match {
            case SpawnJob(name) =>
              context.log.info("Spawning job {}!", name)
              context.spawn(Job(name), name = name)
              Behaviors.same
            case GracefulShutdown =>
              context.log.info("Initiating graceful shutdown...")
              Behaviors.stopped { () =>
                cleanup(context.system.log)
              }
          }
        }
        .receiveSignal { case (context, PostStop) =>
          context.log.info("Master stopped")
          Behaviors.same
        }
    }
  }

  object Job {

    sealed trait Command

    def apply(name: String): Behavior[Command] = {
      Behaviors.receiveSignal { case (context, PostStop) =>
        context.log.info("Worker {} stopped", name)
        Behaviors.same
      }
    }
  }

  import Master._

  val system: ActorSystem[Command] = ActorSystem(Master(), "B7700")

  system ! SpawnJob("a")
  system ! SpawnJob("b")

  Thread.sleep(100)

  system ! GracefulShutdown

  Thread.sleep(100)
  Await.result(system.whenTerminated, 3.seconds)
}
