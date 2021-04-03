package testing

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object Echo {
  final case class Ping(message: String, response: ActorRef[Pong])
  final case class Pong(message: String)

  def apply(): Behavior[Ping] = {
    Behaviors.receiveMessage {
      case Ping(message, replyTo) =>
        replyTo ! Pong(message)
        Behaviors.same
    }
  }
}
