package basic

import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

object MyPersistenceBehavior {
  // Commands
  sealed trait Command extends JsonSerializable
  final case class Add(item: String) extends Command
  case object Clear extends Command
  final case class GetHistory(replyTo: ActorRef[History]) extends Command

  // Events
  sealed trait Event extends JsonSerializable
  final case class Added(data: String) extends Event
  case object Cleared extends Event

  // States
  final case class State(history: History) extends JsonSerializable

  // Models
  final case class History(items: List[String]) extends JsonSerializable {
    def appended(item: String): History = {
      val newItems = (item :: items).take(5)
      History(newItems)
    }
    def isEmpty: Boolean = items.isEmpty
  }
  object History {
    val empty: History = History(Nil)
    def apply(items: String*): History = History(items.toList)
  }

  def apply(id: PersistenceId): Behavior[Command] = {
    EventSourcedBehavior[Command, Event, State](
      persistenceId = id,
      emptyState = State(History.empty),
      commandHandler = commandHandler,
      eventHandler = eventHandler
    ).snapshotWhen {
      case (_, _, 3) => true
      case _         => false
    }
  }

  private def commandHandler(
      state: State,
      command: Command
  ): Effect[Event, State] = {
    command match {
      case Add(item) =>
        Effect.persist(Added(item))
      case Clear =>
        Effect.persist(Cleared)
      case GetHistory(replyTo) =>
        Effect.reply(replyTo)(state.history)
    }
  }

  private def eventHandler(state: State, event: Event): State = {
    event match {
      case Added(item) =>
        state.copy(history = state.history.appended(item))
      case Cleared =>
        State(History.empty)
    }
  }

}
