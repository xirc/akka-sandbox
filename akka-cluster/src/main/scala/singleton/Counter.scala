package singleton

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import serialization.CborSerializable

object Counter {
  sealed trait Command extends CborSerializable
  case object Increment extends Command
  final case class GetValue(replyTo: ActorRef[Int]) extends Command
  case object GoodByeCounter extends Command

  def apply(): Behavior[Command] = {
    apply(0)
  }
  private def apply(value: Int): Behavior[Command] = {
    Behaviors.logMessages {
      Behaviors.receiveMessage {
        case Increment =>
          apply(value + 1)
        case GetValue(replyTo) =>
          replyTo ! value
          Behaviors.same
        case GoodByeCounter =>
          Behaviors.stopped
      }
    }
  }
}
