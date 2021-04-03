package lifecycle

import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object SpawnProtocolExample extends App {

  object HelloWorldMain {
    def apply(): Behavior[SpawnProtocol.Command] = {
      Behaviors.setup { context =>
        // Do something
        SpawnProtocol()
      }
    }
  }

  object HelloWorld {

    final case class Message(message: String, replyTo: ActorRef[Message])

    def apply(): Behavior[Message] = {
      Behaviors.receive { (context, message) =>
        context.log.info("received message: {}", message)
        message.replyTo ! Message(message.message, context.self)
        Behaviors.same
      }
    }
  }

  implicit val system: ActorSystem[SpawnProtocol.Command] =
    ActorSystem(HelloWorldMain(), "hello")

  import system.executionContext

  implicit val timeout: Timeout = 3.seconds

  val greeterFuture: Future[ActorRef[HelloWorld.Message]] = {
    system.ask(
      SpawnProtocol.Spawn(
        behavior = HelloWorld(),
        name = "greeter",
        props = Props.empty,
        _
      )
    )
  }
  val greetedBehavior: Behavior[HelloWorld.Message] = {
    Behaviors.receive[HelloWorld.Message] { (context, message) =>
      context.log.info2(
        "Greeting for {} from {}",
        message.replyTo,
        message.message
      )
      Behaviors.stopped
    }
  }
  val greetedFuture: Future[ActorRef[HelloWorld.Message]] = {
    system.ask(
      SpawnProtocol
        .Spawn(greetedBehavior, name = "greeted", props = Props.empty, _)
    )
  }

  for (greeter <- greeterFuture; greeted <- greetedFuture) {
    greeter ! HelloWorld.Message("Akka", greeted)
  }

  Thread.sleep(1000)
  system.terminate()
}
