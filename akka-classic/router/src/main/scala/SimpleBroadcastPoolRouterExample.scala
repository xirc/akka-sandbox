import akka.actor.ActorSystem
import akka.routing.{BroadcastPool, FromConfig}
import com.typesafe.config.ConfigFactory

object SimpleBroadcastPoolRouterExample extends App {
  val config = ConfigFactory
    .parseString("""
      |akka.actor.deployment {
      |  /router13 {
      |    router = broadcast-pool
      |    nr-of-instances = 5
      |  }
      |}
      |""".stripMargin)
    .withFallback(ConfigFactory.load)
  val system = ActorSystem("system", config)

  val router13 = system.actorOf(FromConfig.props(Worker.props), "router13")
  val router14 =
    system.actorOf(BroadcastPool(5).props(Worker.props), "router14")

  router13 ! "job1"
  router13 ! "job2"

  router14 ! "job3"
  router14 ! "job4"

  Thread.sleep(1000)
  system.terminate()
}
