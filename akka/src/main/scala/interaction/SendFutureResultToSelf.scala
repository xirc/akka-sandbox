package interaction

import akka.Done
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object SendFutureResultToSelf extends App {

  final case class Customer(
      id: String,
      version: Long,
      name: String,
      address: String
  )

  trait CustomerDataAccess {
    def update(value: Customer): Future[Done]
  }

  object CustomerDataAccess {
    def apply(
        processingTime: FiniteDuration
    )(implicit executionContext: ExecutionContext): CustomerDataAccess = {
      new CustomerDataAccess {
        override def update(value: Customer): Future[Done] = {
          Future {
            Thread.sleep(processingTime.toMillis)
            Done
          }
        }
      }
    }
  }

  object CustomerRepository {

    sealed trait Command

    final case class Update(value: Customer, replyTo: ActorRef[UpdateResult])
        extends Command

    sealed trait UpdateResult

    final case class UpdateSuccess(id: String) extends UpdateResult

    final case class UpdateFailure(id: String, reason: String)
        extends UpdateResult

    private final case class WrappedUpdateResult(
        result: UpdateResult,
        replyTo: ActorRef[UpdateResult]
    ) extends Command

    private val MaxOperationsInProgress = 10

    def apply(dataAccess: CustomerDataAccess): Behavior[Command] = {
      apply(dataAccess, operationsInProgress = 0)
    }

    private def apply(
        dataAccess: CustomerDataAccess,
        operationsInProgress: Int
    ): Behavior[Command] = {
      Behaviors.receive { (context, command) =>
        command match {
          case Update(value, replyTo) =>
            if (operationsInProgress == MaxOperationsInProgress) {
              replyTo ! UpdateFailure(
                value.id,
                s"Max $MaxOperationsInProgress concurrent operations supported"
              )
              Behaviors.same
            } else {
              val resultFuture = dataAccess.update(value)
              context.pipeToSelf(resultFuture) {
                case Success(_) =>
                  WrappedUpdateResult(UpdateSuccess(value.id), replyTo)
                case Failure(e) =>
                  WrappedUpdateResult(
                    UpdateFailure(value.id, e.getMessage),
                    replyTo
                  )
              }
              apply(dataAccess, operationsInProgress + 1)
            }
          case WrappedUpdateResult(result, replyTo) =>
            replyTo ! result
            apply(dataAccess, operationsInProgress - 1)
        }
      }
    }
  }

  object Main {
    def apply(): Behavior[CustomerRepository.Command] = {
      Behaviors.setup { context =>
        import context.executionContext
        val dataAccess = CustomerDataAccess(100.millis)
        val repository =
          context.spawn(CustomerRepository(dataAccess), "repository")
        Behaviors.receiveMessage { message =>
          repository ! message
          Behaviors.same
        }
      }
    }
  }

  implicit val timeout: Timeout = 3.seconds
  implicit val system = ActorSystem(Main(), "system")
  import system.executionContext

  val results = for (i <- 1 to 20) yield {
    val id = s"id_${i}"
    val name = s"name_${i}"
    val street = s"street_${i}"
    system.ask(
      CustomerRepository.Update(Customer(id, 1, name, street), _)
    )
  }
  for (result <- results) {
    result.onComplete {
      case Success(value) =>
        println(value)
      case Failure(exception) =>
        println(exception)
    }
  }

  Thread.sleep(1000)
  system.terminate()
}
