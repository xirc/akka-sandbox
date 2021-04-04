import akka.actor.ActorSystem
import akka.routing.{FromConfig, RandomPool}
import com.typesafe.config.ConfigFactory

object SimpleRandomPoolRouterExample extends App {
  val config = ConfigFactory
    .parseString("""
      |akka.actor.deployment {
      |  /router5 {
      |    router = random-pool
      |    nr-of-instances = 5
      |  }
      |}
      |""".stripMargin)
    .withFallback(ConfigFactory.load)
  val system = ActorSystem("system", config)

  val router5 = system.actorOf(FromConfig.props(Worker.props), "router5")
  val router6 = system.actorOf(RandomPool(5).props(Worker.props), "router6")

  router5 ! "job1"
  router5 ! "job2"
  router5 ! "job3"

  router6 ! "job4"
  router6 ! "job5"
  router6 ! "job6"

  system.terminate()
}
