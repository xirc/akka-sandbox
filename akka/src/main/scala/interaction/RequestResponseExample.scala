package interaction

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

object RequestResponseExample extends App {

  object CookieFabric {

    final case class Request(query: String, replyTo: ActorRef[Response])

    final case class Response(result: String)

    def apply(): Behavior[Request] = {
      Behaviors.receive[Request] { (context, message) =>
        message match {
          case Request(query, replyTo) =>
            context.log.info("query: {}", query)
            replyTo ! Response(s"Here are the cookies for [$query]!")
            Behaviors.same
        }
      }
    }
  }

  object Client {

    import CookieFabric._

    def apply(): Behavior[Response] = {
      Behaviors.receive[Response] { (context, message) =>
        message match {
          case Response(result) =>
            context.log.info("result: {}", result)
            Behaviors.stopped
        }
      }
    }
  }

  object Main {

    import CookieFabric._

    def apply(): Behavior[NotUsed] = {
      Behaviors.setup { context =>
        val fabric = context.spawn(CookieFabric(), "fabric")
        val client = context.spawn(Client(), "client")
        fabric ! Request("hello", client)
        Behaviors.same
      }
    }
  }

  implicit val system = ActorSystem(Main(), "system")
  Thread.sleep(100)
  system.terminate()
}
