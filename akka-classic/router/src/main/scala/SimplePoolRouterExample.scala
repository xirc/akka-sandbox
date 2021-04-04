import akka.actor.ActorSystem
import akka.routing.{FromConfig, RoundRobinPool}
import com.typesafe.config.ConfigFactory

object SimplePoolRouterExample extends App {
  val config = ConfigFactory
    .parseString("""
      |akka.actor.deployment {
      |  /router1 {
      |    router = round-robin-pool
      |    nr-of-instances = 5
      |  }
      |}
      |""".stripMargin)
    .withFallback(ConfigFactory.load)
  val system = ActorSystem("system", config)

  val router1 = system.actorOf(FromConfig.props(Worker.props), "router1")
  val router2 = system.actorOf(RoundRobinPool(5).props(Worker.props), "router2")

  router1 ! "job1"
  router1 ! "job2"

  router2 ! "job3"
  router2 ! "job4"

  Thread.sleep(1000)

  system.terminate()
}
