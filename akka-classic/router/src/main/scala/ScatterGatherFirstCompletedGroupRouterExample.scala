import akka.actor.ActorSystem
import akka.pattern.ask
import akka.routing.{FromConfig, ScatterGatherFirstCompletedGroup}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent._
import scala.concurrent.duration._

object ScatterGatherFirstCompletedGroupRouterExample extends App {
  val paths = Workers.makePaths("/user/workers/")
  val config = ConfigFactory
    .parseString(s"""
      |akka.actor.deployment {
      |  /router19 {
      |    router = scatter-gather-group
      |    routees.paths = ${paths.mkString("[", ",", "]")}
      |  }
      |}
      |""".stripMargin)
    .withFallback(ConfigFactory.load)
  val system = ActorSystem("system", config)

  val router19 = system.actorOf(FromConfig.props(), "router19")
  val router20 = system.actorOf(
    ScatterGatherFirstCompletedGroup(paths, within = 10.seconds).props(),
    "router20"
  )
  val workers = system.actorOf(Workers.props, "workers")

  // Waiting for that workers wake up
  Thread.sleep(1000)

  implicit val timeout: Timeout = 3.seconds

  val result1 = router19 ? "job1"
  println(Await.result(result1, 1.second))

  val result2 = router20 ? "job2"
  println(Await.result(result2, 1.second))

  Thread.sleep(1000)
  system.terminate()
}
