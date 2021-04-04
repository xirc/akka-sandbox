import akka.actor.ActorSystem
import akka.routing.{BalancingPool, FromConfig}
import com.typesafe.config.ConfigFactory

object SimpleBalancingPoolRouterExample extends App {
  val config = ConfigFactory
    .parseString("""
      |akka.actor.deployment {
      |  /router9 {
      |    router = balancing-pool
      |    nr-of-instances = 5
      |  }
      |}
      |""".stripMargin)
    .withFallback(ConfigFactory.load)
  val system = ActorSystem("system", config)

  val router9 = system.actorOf(FromConfig.props(Worker.props), "router9")
  val router10 =
    system.actorOf(BalancingPool(5).props(Worker.props), "router10")

  for (i <- 1 to 10) {
    router9 ! s"job${i}"
  }
  for (i <- 11 to 20) {
    router10 ! s"job${i}"
  }

  Thread.sleep(1000)
  system.terminate()
}
