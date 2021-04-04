import akka.actor.ActorSystem
import akka.routing.{FromConfig, RoundRobinGroup}
import com.typesafe.config.ConfigFactory

object SimpleGroupRouterExample extends App {
  val paths = Workers.makePaths("/user/workers/")
  val config = ConfigFactory
    .parseString(s"""
      |akka.actor.deployment {
      |  /router3 {
      |    router = round-robin-group
      |    routees.paths = ${paths.mkString("[", ",", "]")}
      |  }
      |}
      |""".stripMargin)
    .withFallback(ConfigFactory.load)
  val system = ActorSystem("system", config)

  val router3 = system.actorOf(FromConfig.props(), "router3")
  val router4 = system.actorOf(RoundRobinGroup(paths).props(), "router4")
  val workers = system.actorOf(Workers.props, "workers")

  // Waiting for that workers wake up
  Thread.sleep(1000)

  router3 ! "job1"
  router3 ! "job2"
  router3 ! "job3"

  router4 ! "job4"
  router4 ! "job5"
  router4 ! "job6"

  Thread.sleep(1000)

  system.terminate()
}
