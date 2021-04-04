import akka.actor.{ActorSystem, PoisonPill}
import akka.routing.{Broadcast, RoundRobinGroup, RoundRobinPool}

object PoisonPillMessageExample extends App {
  val system = ActorSystem("system")

  // Terminate router1
  val router1 = system.actorOf(RoundRobinPool(5).props(Worker.props))
  router1 ! "job1"
  router1 ! PoisonPill
  Thread.sleep(100)
  router1 ! "job2"

  // terminate routees of router2, and then router2.
  val workerA = system.actorOf(Worker.props)
  val workerB = system.actorOf(Worker.props)
  val router2 = system.actorOf(
    RoundRobinGroup(
      List(workerA, workerB).map(_.path.toString)
    ).props()
  )
  router2 ! "job3"
  router2 ! Broadcast(PoisonPill)
  Thread.sleep(100)
  router2 ! "job4"

  Thread.sleep(1000)
  system.terminate()

}
