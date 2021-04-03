package interaction

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

object AdaptedResponseExample extends App {

  object Backend {

    sealed trait Request

    final case class StartTranslationJob(taskId: Int, site: String, replyTo: ActorRef[Response]) extends Request

    sealed trait Response

    final case class JobStarted(taskId: Int) extends Response

    final case class JobProgress(taskId: Int, progress: Double) extends Response

    final case class JobCompleted(taskId: Int, result: String) extends Response

    def apply(): Behavior[Request] = {
      Behaviors.receiveMessage {
        case job: StartTranslationJob =>
          job.replyTo ! JobStarted(job.taskId)
          for (p <- 0 to 100 by 10) {
            job.replyTo ! JobProgress(job.taskId, p.toDouble)
          }
          job.replyTo ! JobCompleted(job.taskId, job.site.toUpperCase)
          Behaviors.same
      }
    }
  }

  object Frontend {

    sealed trait Command

    final case class Translate(site: String, replyTo: ActorRef[String]) extends Command

    private final case class WrappedBackendResponse(response: Backend.Response) extends Command

    def apply(backend: ActorRef[Backend.Request]): Behavior[Command] = {
      Behaviors.setup[Command] { context =>
        val backendResponseMapper: ActorRef[Backend.Response] =
          context.messageAdapter(WrappedBackendResponse)

        def active(inProgress: Map[Int, ActorRef[String]], count: Int): Behavior[Command] = {
          Behaviors.receiveMessage[Command] {
            case Translate(site, replyTo) =>
              val taskId = count + 1
              backend ! Backend.StartTranslationJob(taskId, site, backendResponseMapper)
              active(inProgress + (taskId -> replyTo), taskId)
            case wrapped: WrappedBackendResponse =>
              wrapped.response match {
                case Backend.JobStarted(taskId) =>
                  context.log.info("Started {}", taskId)
                  Behaviors.same
                case Backend.JobProgress(taskId, progress) =>
                  context.log.info("Progress {}: {}", taskId, progress)
                  Behaviors.same
                case Backend.JobCompleted(taskId, result) =>
                  context.log.info("Completed {}: {}", taskId, result)
                  inProgress(taskId) ! result
                  active(inProgress - taskId, count)
              }
          }
        }

        active(inProgress = Map.empty, count = 0)
      }
    }
  }

  object Main {
    def apply(): Behavior[Frontend.Command] = {
      Behaviors.setup { context =>
        val backend = context.spawn(Backend(), "backend")
        val frontend = context.spawn(Frontend(backend), "frontend")
        val client = context.spawn(Behaviors.logMessages(Behaviors.ignore[String]), "client")
        frontend ! Frontend.Translate("hello", client)
        Behaviors.empty
      }
    }
  }

  val system = ActorSystem(Main(), "system")
  Thread.sleep(100)
  system.terminate()
}
