package shutdown

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object MyActor {
  sealed trait Message
  final case class Stop(replyTo: ActorRef[Done]) extends Message

  def apply(): Behavior[Message] = {
    Behaviors.logMessages {
      Behaviors.receiveMessage {
        case Stop(replyTo) =>
          replyTo ! Done
          Behaviors.stopped
      }
    }
  }
}
