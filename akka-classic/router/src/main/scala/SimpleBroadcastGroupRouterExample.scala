import akka.actor.ActorSystem
import akka.routing.{BroadcastGroup, FromConfig}
import com.typesafe.config.ConfigFactory

object SimpleBroadcastGroupRouterExample extends App {
  val paths = Workers.makePaths("/user/workers/")
  val config = ConfigFactory
    .parseString(s"""
      |akka.actor.deployment {
      |  /router15 {
      |    router = broadcast-group
      |    routees.paths = ${paths.mkString("[", ",", "]")}
      |  }
      |}
      |""".stripMargin)
    .withFallback(ConfigFactory.load)
  val system = ActorSystem("system", config)

  val router15 = system.actorOf(FromConfig.props(), "router15")
  val router16 = system.actorOf(BroadcastGroup(paths).props(), "router16")
  val workers = system.actorOf(Workers.props, "workers")

  // Waiting for that workers wake up
  Thread.sleep(1000)

  router15 ! "job1"
  router15 ! "job2"

  router16 ! "job3"
  router16 ! "job3"

  Thread.sleep(1000)
  system.terminate()
}
