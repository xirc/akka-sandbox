package sharding

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import serialization.CborSerializable

object Counter {
  sealed trait Command extends CborSerializable
  case object Increment extends Command
  final case class GetValue(replyTo: ActorRef[Int]) extends Command

  def apply(entityId: String): Behavior[Command] = {
    def updated(value: Int): Behavior[Command] = {
      Behaviors.receiveMessage {
        case Increment =>
          updated(value + 1)
        case GetValue(replyTo) =>
          replyTo ! value
          Behaviors.same
      }
    }
    updated(0)
  }
}
