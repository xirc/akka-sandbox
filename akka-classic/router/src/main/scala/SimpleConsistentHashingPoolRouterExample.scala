import akka.actor.ActorSystem
import akka.routing.{ConsistentHashingPool, FromConfig}
import akka.routing.ConsistentHashingRouter.{
  ConsistentHashMapping,
  ConsistentHashable,
  ConsistentHashableEnvelope
}
import com.typesafe.config.ConfigFactory

object SimpleConsistentHashingPoolRouterExample extends App {
  final case class Evict(key: String)
  final case class Get(key: String) extends ConsistentHashable {
    override def consistentHashKey: Any = key
  }
  final case class Entry(key: String, value: String)
  def hashMapping: ConsistentHashMapping = { case Evict(key) =>
    key
  }

  val config = ConfigFactory
    .parseString("""
      |akka.actor.deployment {
      |  /router25 {
      |    router = consistent-hashing-pool
      |    nr-of-instances = 5
      |    virtual-nodes-factor = 10
      |  }
      |}
      |""".stripMargin)
    .withFallback(ConfigFactory.load)
  val system = ActorSystem("system", config)

  val router = system.actorOf(
    ConsistentHashingPool(10, hashMapping = hashMapping).props(Worker.props),
    "router"
  )
  val router25 = system.actorOf(FromConfig.props(Worker.props), "router25")
  val router26 = system.actorOf(
    ConsistentHashingPool(10, hashMapping = hashMapping).props(Worker.props),
    "router26"
  )

  // Use ConsistentHashableEnvelope
  router ! ConsistentHashableEnvelope(
    message = Entry("hello", "HELLO"),
    hashKey = "hello"
  )
  router ! ConsistentHashableEnvelope(
    message = Entry("hi", "HI"),
    hashKey = "hi"
  )
  router25 ! ConsistentHashableEnvelope(
    message = Entry("hello", "HELLO"),
    hashKey = "hello"
  )
  router25 ! ConsistentHashableEnvelope(
    message = Entry("hello", "HELLO"),
    hashKey = "hello"
  )
  router26 ! ConsistentHashableEnvelope(
    message = Entry("hello", "HELLO"),
    hashKey = "hello"
  )
  router26 ! ConsistentHashableEnvelope(
    message = Entry("hello", "HELLO"),
    hashKey = "hello"
  )

  // Use ConsistentHashable
  router ! Get("hello")
  router ! Get("hi")
  router25 ! Get("hello")
  router25 ! Get("hi")
  router26 ! Get("hello")
  router26 ! Get("hi")

  // Use ConsistentHashMapping
  router ! Evict("hi")
  // router25 ! Evict("hi")
  router26 ! Evict("hi")

  Thread.sleep(1000)
  system.terminate()
}
