import akka.actor.{ActorSystem, Kill}
import akka.routing.{Broadcast, RoundRobinGroup, RoundRobinPool}

object KillMessageExample extends App {
  val system = ActorSystem("system")

  val router1 = system.actorOf(RoundRobinPool(5).props(Worker.props), "router1")
  router1 ! Kill

  val workerA = system.actorOf(Workers.props, "workerA")
  val router2 = system.actorOf(
    RoundRobinGroup(
      List(workerA).map(_.path.toString)
    ).props(),
    "router2"
  )
  router2 ! Broadcast(Kill)
  Thread.sleep(300)

  Thread.sleep(1000)
  system.terminate()
}
