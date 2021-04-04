import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch.Dispatchers
import akka.japi.Util.immutableSeq
import akka.routing.{
  FromConfig,
  Group,
  RoundRobinRoutingLogic,
  Routee,
  Router,
  RoutingLogic,
  SeveralRoutees
}
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.immutable

object CustomRouterExample extends App {
  final class RedundancyRoutingLogic(nrCopies: Int) extends RoutingLogic {
    private val roundRobin = RoundRobinRoutingLogic()
    override def select(message: Any, routees: IndexedSeq[Routee]): Routee = {
      val targets =
        (1 to nrCopies).map(_ => roundRobin.select(message, routees))
      SeveralRoutees(targets)
    }
  }
  final case class RedundancyGroup(
      routeePaths: immutable.Iterable[String],
      nrCopies: Int
  ) extends Group {
    def this(config: Config) = {
      this(
        routeePaths = immutableSeq(config.getStringList("routees.paths")),
        nrCopies = config.getInt("nr-copies")
      )
    }

    override def paths(system: ActorSystem): immutable.Iterable[String] =
      routeePaths
    override def createRouter(system: ActorSystem): Router = {
      new Router(new RedundancyRoutingLogic(nrCopies))
    }
    override def routerDispatcher: String = Dispatchers.DefaultDispatcherId
  }

  //
  // Use Routing Logic
  //

  final case class TestRoutee(n: Int) extends Routee {
    override def send(message: Any, sender: ActorRef): Unit = ()
  }
  val logic = new RedundancyRoutingLogic(nrCopies = 3)
  val routees = for (n <- 1 to 7) yield TestRoutee(n)

  val r1 = logic.select("message1", routees)
  println(r1.asInstanceOf[SeveralRoutees].routees) // 1, 2, 3

  val r2 = logic.select("message2", routees)
  println(r2.asInstanceOf[SeveralRoutees].routees) // 4, 5, 6

  val r3 = logic.select("message3", routees)
  println(r3.asInstanceOf[SeveralRoutees].routees) // 7, 1, 2

  //
  // Use Router
  //

  val nrOfWorkers = 5
  val paths = Workers.makePaths("/user/workers/", nrOfWorkers)
  val config = ConfigFactory
    .parseString(s"""
      |akka.actor.deployment {
      |  /router2 {
      |    router = "${classOf[RedundancyGroup].getName}"
      |    routees.path = ${paths.mkString("[", ",", "]")}
      |    nr-copies = 3
      |  }
      |}
      |""".stripMargin)
    .withFallback(ConfigFactory.load)
  val system = ActorSystem("system", config)

  val router1 = system.actorOf(
    RedundancyGroup(paths, nrCopies = 3).props(),
    name = "router1"
  )
  val router2 = system.actorOf(FromConfig.props(), "router2")
  val workers = system.actorOf(Workers.props(nrOfWorkers), "workers")

  // Waiting for that workers wake up
  Thread.sleep(1000)

  router1 ! "job1"
  router1 ! "job2"
  router1 ! "job3"

  router2 ! "job4"
  router2 ! "job5"
  router2 ! "job6"

  Thread.sleep(1000)
  system.terminate()
}
