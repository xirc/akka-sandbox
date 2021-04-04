import akka.actor.ActorSystem
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.routing.{ConsistentHashingGroup, FromConfig}
import com.typesafe.config.ConfigFactory

object SimpleConsistentHashingGroupRouterExample extends App {
  val paths = Workers.makePaths("/user/workers/")
  val config = ConfigFactory
    .parseString(s"""
      |akka.actor.deployment {
      |  /router27 {
      |    router = consistent-hashing-group
      |    routees.paths = ${paths.mkString("[", ",", "]")}
      |  }
      |}
      |""".stripMargin)
    .withFallback(ConfigFactory.load)
  val system = ActorSystem("system", config)

  val router27 = system.actorOf(FromConfig.props(), "router27")
  val router28 =
    system.actorOf(ConsistentHashingGroup(paths).props(), "router28")
  val workers = system.actorOf(Workers.props, "workers")

  // Waiting for that workers wake up
  Thread.sleep(1000)

  router27 ! ConsistentHashableEnvelope(message = "job1", "job1")
  router27 ! ConsistentHashableEnvelope(message = "job2", "job2")
  router27 ! ConsistentHashableEnvelope(message = "job1", "job1")
  router27 ! ConsistentHashableEnvelope(message = "job2", "job2")

  router28 ! ConsistentHashableEnvelope(message = "job3", "job3")
  router28 ! ConsistentHashableEnvelope(message = "job4", "job4")
  router28 ! ConsistentHashableEnvelope(message = "job3", "job3")
  router28 ! ConsistentHashableEnvelope(message = "job4", "job4")

  Thread.sleep(1000)
  system.terminate()
}
