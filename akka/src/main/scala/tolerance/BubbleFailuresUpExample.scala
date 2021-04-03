package tolerance

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{
  ActorRef,
  ActorSystem,
  Behavior,
  DeathPactException,
  SupervisorStrategy
}

object BubbleFailuresUpExample extends App {

  object Protocol {
    sealed trait Command
    final case class Fail(text: String) extends Command
    final case class Hello(text: String, replyTo: ActorRef[String])
        extends Command
  }

  object Worker {
    import Protocol._
    def apply(): Behavior[Command] = {
      Behaviors.receiveMessage {
        case Fail(text) =>
          throw new RuntimeException(text)
        case Hello(text, replyTo) =>
          replyTo ! text
          Behaviors.same
      }
    }
  }

  object MiddleManagement {
    import Protocol._
    def apply(): Behavior[Command] = {
      Behaviors.setup { context =>
        context.log.info("Middle management starting up")
        val child = context.spawn(Worker(), "child")
        context.watch(child)
        Behaviors.receiveMessage { message =>
          child ! message
          Behaviors.same
        }
      }
    }
  }

  object Boss {
    import Protocol._
    def apply(): Behavior[Command] = {
      Behaviors
        .supervise {
          Behaviors.setup[Command] { context =>
            context.log.info("Boss starting up")
            val middle = context.spawn(MiddleManagement(), "middle-management")
            context.watch(middle)
            Behaviors.receiveMessage { message =>
              middle ! message
              Behaviors.same
            }
          }
        }
        .onFailure[DeathPactException](SupervisorStrategy.restart)
    }
  }

  object Main {
    import Protocol._
    def apply(): Behavior[Command] = {
      Behaviors.setup { context =>
        val boss = context.spawn(Boss(), "boss")
        val client = context.spawn(
          Behaviors.logMessages(Behaviors.ignore[String]),
          "client"
        )
        Behaviors.receiveMessage[Command] {
          case hello: Hello =>
            boss ! hello.copy(replyTo = client)
            Behaviors.same
          case fail: Fail =>
            boss ! fail
            Behaviors.same
        }
      }
    }
  }

  val system = ActorSystem(Main(), "system")

  import Protocol._

  system ! Hello("hello", system.deadLetters)
  system ! Fail("bomb")

  Thread.sleep(1000)

  system ! Hello("world", system.deadLetters)

  system.terminate()
}
