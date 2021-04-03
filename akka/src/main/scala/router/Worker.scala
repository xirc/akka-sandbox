package router

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}

object Worker {
  sealed trait Command
  final case class DoLog(text: String) extends Command

  def apply(): Behavior[Command] = {
    Behaviors.setup { context =>
      context.log.info("Starting worker")
      Behaviors.receiveMessage { case DoLog(text) =>
        context.log.info2("{} - Got message {}", context.self.path.name, text)
        Behaviors.same
      }
    }
  }
}
