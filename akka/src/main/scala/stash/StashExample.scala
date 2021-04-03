package stash

import akka.{Done, NotUsed}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait DB {
  def save(id: String, value: Int): Future[Done]
  def load(id: String): Future[Int]
}

object DB {
  def apply(): DB = new DB {
    private val store: mutable.Map[String,Int] = mutable.Map.empty
    override def save(id: String, value: Int): Future[Done] = {
      Future.successful {
        store += id -> value
        Done
      }
    }
    override def load(id: String): Future[Int] = {
      Future.successful {
        store.getOrElse(id, 0)
      }
    }
  }
}

object DataAccess {
  sealed trait Command
  final case class Save(value: Int, replyTo: ActorRef[Done]) extends Command
  final case class Get(replyTo: ActorRef[Int]) extends Command
  private final case class InitialState(value: Int) extends Command
  private case object SaveSuccess extends Command
  private final case class DBError(cause: Throwable) extends Command

  def apply(id: String, db: DB): Behavior[Command] = {
    Behaviors.logMessages {
      Behaviors.withStash(100) { buffer =>
        Behaviors.setup[Command] { context =>
          new DataAccess(context, buffer, id, db).start()
        }
      }
    }
  }
}

class DataAccess
(
  context: ActorContext[DataAccess.Command],
  buffer: StashBuffer[DataAccess.Command],
  id: String,
  db: DB
) {
  import DataAccess._

  private def start(): Behavior[Command] = {
    context.pipeToSelf(db.load(id)) {
      case Success(value) => InitialState(value)
      case Failure(cause) => DBError(cause)
    }
    Behaviors.receiveMessage {
      case InitialState(value) =>
        buffer.unstashAll(active(value))
      case DBError(cause) =>
        throw cause
      case other =>
        buffer.stash(other)
        Behaviors.same
    }
  }
  private def active(state: Int): Behavior[Command] = {
    Behaviors.receiveMessagePartial {
      case Get(replyTo) =>
        replyTo ! state
        Behaviors.same
      case Save(value, replyTo) =>
        context.pipeToSelf(db.save(id, value)) {
          case Success(_) => SaveSuccess
          case Failure(cause) => DBError(cause)
        }
        saving(value, replyTo)
    }
  }
  private def saving(state: Int, replyTo: ActorRef[Done]): Behavior[Command] = {
    Behaviors.receiveMessage {
      case SaveSuccess =>
        replyTo ! Done
        buffer.unstashAll(active(state))
      case DBError(cause) =>
        throw cause
      case other =>
        buffer.stash(other)
        Behaviors.same
    }
  }
}

object StashExample extends App {
  object Main {
    def apply(): Behavior[NotUsed] = {
      import DataAccess._
      val db = DB()
      Behaviors.setup { context =>
        val alice = context.spawn(DataAccess("alice", db), "alice")
        val john = context.spawn(DataAccess("john", db), "john")
        val logger = context.spawn(Behaviors.logMessages(Behaviors.ignore[Any]), "logger")

        alice ! Save(1, logger)
        alice ! Get(logger)

        john ! Get(logger)
        john ! Save(2, logger)

        john ! Get(logger)
        alice ! Get(logger)

        Behaviors.same
      }
    }
  }

  val system = ActorSystem(Main(), "system")
  Thread.sleep(1000)
  system.terminate()
}
