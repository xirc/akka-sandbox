import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}

import scala.concurrent.Future

object Main extends App {
  val system = ActorSystem("ClusterSystem")
  CoordinatedShutdown(system).addTask(
    CoordinatedShutdown.PhaseActorSystemTerminate,
    "task"
  ) { () =>
    println(
      s"CoordinatedShutdown - {${CoordinatedShutdown.PhaseActorSystemTerminate}}"
    )
    Future.successful(Done)
  }
  val listener = system.actorOf(SimpleClusterListener.props)
}
