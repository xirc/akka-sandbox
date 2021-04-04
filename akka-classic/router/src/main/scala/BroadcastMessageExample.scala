import akka.actor.ActorSystem
import akka.routing.{Broadcast, RoundRobinPool}

object BroadcastMessageExample extends App {
  val system = ActorSystem("system")

  val router = system.actorOf(
    RoundRobinPool(5).props(Worker.props)
  )

  router ! Broadcast("hi")

  Thread.sleep(1000)
  system.terminate()
}
