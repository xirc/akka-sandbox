import akka.actor.ActorSystem
import akka.routing.{FromConfig, RandomGroup}
import com.typesafe.config.ConfigFactory

object SimpleRandomGroupRouterExample extends App {
  val paths = Workers.makePaths("/user/workers/")
  val config = ConfigFactory
    .parseString(s"""
      |akka.actor.deployment {
      |  /router7 {
      |    router = random-group
      |    routees.paths = ${paths.mkString("[", ",", "]")}
      |  }
      |}""".stripMargin)
    .withFallback(ConfigFactory.load)
  val system = ActorSystem("system", config)

  val router7 = system.actorOf(FromConfig.props(), "router7")
  val router8 = system.actorOf(RandomGroup(paths).props(), "router8")
  val workers = system.actorOf(Workers.props, "workers")

  // Waiting for that workers wake up
  Thread.sleep(1000)

  router7 ! "job1"
  router7 ! "job2"
  router7 ! "job3"

  router8 ! "job4"
  router8 ! "job5"
  router8 ! "job6"

  Thread.sleep(1000)

  system.terminate()
}
