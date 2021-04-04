import akka.actor.ActorSystem
import akka.routing.{DefaultResizer, FromConfig, RoundRobinPool}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

object DefaultResizerExample extends App {
  val config = ConfigFactory
    .parseString("""
      |akka.actor.deployment {
      |  /router29 {
      |    router = round-robin-pool
      |    resizer {
      |      lower-bound = 2
      |      upper-bound = 15
      |      messages-per-resize = 3
      |    }
      |  }
      |}
      |""".stripMargin)
    .withFallback(ConfigFactory.load)
  val system = ActorSystem("system", config)

  val router29 =
    system.actorOf(FromConfig.props(Worker.props(20.millis)), "router29")
  val resizer =
    DefaultResizer(lowerBound = 2, upperBound = 15, messagesPerResize = 3)
  val router30 = system.actorOf(
    RoundRobinPool(5, Some(resizer)).props(Worker.props(20.millis)),
    "router30"
  )

  for (x <- 1 to 1000) {
    router29 ! x
  }
  Thread.sleep(5000)

  for (x <- 1001 to 2000) {
    router30 ! x
  }
  Thread.sleep(5000)

  system.terminate()
}
