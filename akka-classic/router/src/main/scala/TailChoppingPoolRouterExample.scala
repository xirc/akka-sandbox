import akka.actor.ActorSystem
import akka.pattern.ask
import akka.routing.{FromConfig, TailChoppingPool}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent._
import scala.concurrent.duration._

object TailChoppingPoolRouterExample extends App {
  val config = ConfigFactory
    .parseString("""
      |akka.actor.deployment {
      |  /router21 {
      |    router = tail-chopping-pool
      |    nr-of-instances = 5
      |    within = 10 seconds
      |    tail-chopping-router.interval = 10 millis
      |  }
      |}
      |""".stripMargin)
    .withFallback(ConfigFactory.load)
  val system = ActorSystem("system", config)

  val router21 = system.actorOf(FromConfig.props(Worker.props), "router21")
  val router22 = system.actorOf(
    TailChoppingPool(10, within = 10.seconds, interval = 20.millis)
      .props(Worker.props),
    "router22"
  )

  implicit val timeout: Timeout = 3.seconds

  val result1 = router21 ? "job1"
  println(Await.result(result1, 1.second))

  val result2 = router22 ? "job2"
  println(Await.result(result2, 1.second))

  Thread.sleep(1000)
  system.terminate()
}
