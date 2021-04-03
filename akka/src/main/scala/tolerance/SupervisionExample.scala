package tolerance

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed._

private object SupervisionExample extends App {
  private object Counter {
    sealed trait Command
    final case class Increment(delta: Int) extends Command
    final case class GetCount(replyTo: ActorRef[GetCountResponse])
        extends Command
    final case class GetCountResponse(name: String, value: Int)
    final case class InjectException(exception: Throwable) extends Command

    def apply(): Behavior[Command] = {
      Counter(0)
    }
    private def apply(count: Int): Behavior[Command] = {
      Behaviors.receive[Command] { (context, message) =>
        message match {
          case Increment(delta) =>
            Counter(count + delta)
          case GetCount(replyTo) =>
            replyTo ! GetCountResponse(context.self.path.name, count)
            Behaviors.same
          case InjectException(exception) =>
            throw exception
        }
      }
    }
  }

  private object CompositeCounter {
    import Counter._
    def apply(): Behavior[Command] = {
      Behaviors.setup { context =>
        val counter1 = context.spawn(Counter(), "counter1")
        val counter2 = context.spawn(
          Behaviors.supervise(Counter()).onFailure(SupervisorStrategy.restart),
          "counter2"
        )
        val counter3 = context.spawn(
          Behaviors.supervise(Counter()).onFailure(SupervisorStrategy.resume),
          "counter3"
        )
        Behaviors.receiveMessage { message =>
          counter1 ! message
          counter2 ! message
          counter3 ! message
          Behaviors.same
        }
      }
    }
  }

  private object Main {
    def apply(): Behavior[NotUsed] = {
      import Counter._
      Behaviors.setup { context =>
        val client = context.spawn(
          Behaviors.logMessages(Behaviors.ignore[GetCountResponse]),
          "client"
        )
        val counter = context.spawn(CompositeCounter(), "counters")

        counter ! Increment(1)
        counter ! GetCount(client)
        counter ! InjectException(new IllegalStateException())
        counter ! GetCount(client)
        counter ! Increment(10)
        counter ! GetCount(client)

        Behaviors.same
      }
    }
  }

  implicit val system = ActorSystem(Main(), "system")
  Thread.sleep(100)
  system.terminate()
}
