import akka.actor.ActorSystem
import akka.pattern.ask
import akka.routing.{FromConfig, TailChoppingGroup}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent._
import scala.concurrent.duration._

object TailChoppingGroupRouterExample extends App {
  val paths = Workers.makePaths("/user/workers/")
  val config = ConfigFactory
    .parseString(s"""
      |akka.actor.deployment {
      |  /router23 {
      |    router = tail-chopping-group
      |    routees.paths = ${paths.mkString("[", ",", "]")}
      |    within = 10 seconds
      |    tail-chopping-router.interval = 20 millis
      |  }
      |}
      |""".stripMargin)
    .withFallback(ConfigFactory.load)
  val system = ActorSystem("system", config)

  val router23 = system.actorOf(FromConfig.props(), "router23")
  val router24 = system.actorOf(
    TailChoppingGroup(paths, within = 10.seconds, interval = 20.millis).props(),
    "router24"
  )
  val workers = system.actorOf(Workers.props, "workers")

  // Waiting for that workers wake up
  Thread.sleep(1000)

  implicit val timeout: Timeout = 3.seconds

  val result1 = router23 ? "job1"
  println(Await.result(result1, 1.second))

  val result2 = router24 ? "job2"
  println(Await.result(result2, 1.second))

  Thread.sleep(1000)
  system.terminate()
}
