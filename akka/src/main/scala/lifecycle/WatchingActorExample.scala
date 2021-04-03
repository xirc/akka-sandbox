package lifecycle

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed._

object WatchingActorExample extends App {

  object Master {

    sealed trait Command

    final case class SpawnJob(name: String) extends Command

    final case class Execute(name: String, command: Job.Command) extends Command

    def apply(): Behavior[Command] = {
      apply(Nil)
    }

    private def apply(jobs: Seq[ActorRef[Job.Command]]): Behavior[Command] = {
      Behaviors.receive[Command] { (context, message) =>
        message match {
          case SpawnJob(name) =>
            context.log.info("Spawning job {}!", name)
            val job = context.spawn(Job(name), name = name)
            context.watch(job)
            apply(job +: jobs)
          case Execute(name, command) =>
            val jobOpt = jobs.find(_.path.name == name)
            jobOpt.foreach(_ ! command)
            Behaviors.same
        }
      }.receiveSignal {
        case (context, Terminated(job)) =>
          context.log.info("Job stopped: {}", job.path.name)
          apply(jobs.filterNot(_ == job))
      }
    }
  }

  object Job {

    sealed trait Command

    case object Terminate extends Command

    def apply(name: String): Behavior[Command] = {
      Behaviors.receiveMessage[Command] {
        case Terminate =>
          Behaviors.stopped
      } receiveSignal {
        case (context, PostStop) =>
          context.log.info("Worker {} stopped", name)
          Behaviors.same
      }
    }
  }

  import Master._

  val system: ActorSystem[Master.Command] = ActorSystem(Master(), "master")

  system ! SpawnJob("abc")
  system ! SpawnJob("def")

  system ! Execute("abc", Job.Terminate)
  system ! Execute("def", Job.Terminate)

  Thread.sleep(100)
  system.terminate()
}
