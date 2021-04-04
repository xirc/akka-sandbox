import akka.actor.ActorSystem
import akka.routing.{FromConfig, SmallestMailboxPool}
import com.typesafe.config.ConfigFactory

object SimpleSmallestMailboxPoolRouterExample extends App {
  val config = ConfigFactory
    .parseString("""
      |akka.actor.deployment {
      |  /router11 {
      |    router = smallest-mailbox-pool
      |    nr-of-instances = 5
      |  }
      |}
      |""".stripMargin)
    .withFallback(ConfigFactory.load)
  val system = ActorSystem("system", config)

  val router11 = system.actorOf(FromConfig.props(Worker.props), "router11")
  val router12 =
    system.actorOf(SmallestMailboxPool(5).props(Worker.props), "router12")

  for (i <- 1 to 10) {
    router11 ! s"job${i}"
  }
  for (i <- 11 to 20) {
    router12 ! s"job${i}"
  }

  Thread.sleep(1000)
  system.terminate()
}
