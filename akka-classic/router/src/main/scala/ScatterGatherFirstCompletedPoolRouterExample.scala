import akka.actor.ActorSystem
import akka.pattern.ask
import akka.routing.{FromConfig, ScatterGatherFirstCompletedPool}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._

object ScatterGatherFirstCompletedPoolRouterExample extends App {
  val config = ConfigFactory
    .parseString("""
      |akka.actor.deployment {
      |  /router17 {
      |    router = scatter-gather-pool
      |    nr-of-instances = 5
      |    within = 10 seconds
      |  }
      |}
      |""".stripMargin)
    .withFallback(ConfigFactory.load)
  val system = ActorSystem("system", config)

  val router17 = system.actorOf(FromConfig.props(Worker.props), "router17")
  val router18 = system.actorOf(
    ScatterGatherFirstCompletedPool(5, within = 10.seconds).props(Worker.props),
    "router18"
  )

  implicit val timeout: Timeout = 3.seconds

  val result1 = router17 ? "job1"
  println(Await.result(result1, 1.second))

  val result2 = router18 ? "job2"
  println(Await.result(result2, 1.second))

  Thread.sleep(1000)
  system.terminate()
}
