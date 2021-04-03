package example

import akka.actor.{ActorSystem, ExtendedActorSystem}
import akka.serialization.Serialization
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class ActorRefSerializeSpec
    extends TestKit(ActorSystem("actor-ref-serialize-test"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

  "serialize and deserialize local actor ref" in {
    // https://doc.akka.io/docs/akka/current/serialization-classic.html
    // https://stackoverflow.com/questions/44875606/akka-doc-is-unclear-about-how-to-get-an-extendedactorsystem-to-deserialize-actor
    // https://doc.akka.io/docs/akka/current/serialization.html#serializing-actorrefs
    // Use ActorRefResolver, this is not recommended technique in my opinion.
    val actorRef = system.actorOf(TestActors.echoActorProps, "mike")
    val serializedRef = Serialization.serializedActorPath(actorRef)
    val deserializedRef = system
      .asInstanceOf[ExtendedActorSystem]
      .provider
      .resolveActorRef(serializedRef)

    deserializedRef ! "hi"
    expectMsg("hi")

    watch(actorRef)
    system.stop(actorRef)
    expectTerminated(actorRef)

    deserializedRef ! "hi"
    expectNoMessage()
  }
}
