import akka.actor.ActorSystem
import akka.routing.FromConfig
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

object OptimalSizeExploringResizerExample extends App {
  val config = ConfigFactory
    .parseString("""
      |akka.actor.deployment {
      |  /router31 {
      |    router = round-robin-pool
      |    optimal-size-exploring-resizer {
      |      enabled = on
      |      action-interval = 100 millis
      |      downsize-after-underutilized-for = 3 seconds
      |    }
      |  }
      |}
      |""".stripMargin)
    .withFallback(ConfigFactory.load)
  val system = ActorSystem("system", config)

  val router31 =
    system.actorOf(FromConfig.props(Worker.props(20.millis)), "router31")

  for (x <- 1 to 1000) {
    Thread.sleep(10)
    router31 ! x
  }
  Thread.sleep(5000)

  println("Wait for underutilized...")
  Thread.sleep(3000)

  for (x <- 1 to 1000) {
    Thread.sleep(10)
    router31 ! x
  }
  Thread.sleep(5000)

  system.terminate()
}
