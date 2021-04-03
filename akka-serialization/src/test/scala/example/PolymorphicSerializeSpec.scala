package example

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

object PolymorphicSerializeSpec {
  final case class Zoo(primaryAttraction: Animal) extends JsonSerializable
  // Don't use JsonTypeInfo.Id.CLASS for security
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes(
    Array(
      new JsonSubTypes.Type(value = classOf[Lion], name = "lion"),
      new JsonSubTypes.Type(value = classOf[Elephant], name = "elephant")
    )
  )
  sealed trait Animal
  final case class Lion(name: String) extends Animal
  final case class Elephant(name: String, age: Int) extends Animal
}

final class PolymorphicSerializeSpec
    extends TestKit(ActorSystem("polymorphic-serialize-test"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {
  import PolymorphicSerializeSpec._

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "polymorphic type can be serialized/deserialized" in {
    val actor = system.actorOf(TestActors.echoActorProps)
    actor ! Zoo(Lion("the lion king"))
    expectMsg(Zoo(Lion("the lion king")))

    actor ! Zoo(Elephant("the elephant man", 55))
    expectMsg(Zoo(Elephant("the elephant man", 55)))
  }

}
