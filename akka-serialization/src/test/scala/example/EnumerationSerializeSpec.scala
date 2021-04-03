package example

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

object EnumerationSerializeSpec {
  object Planet extends Enumeration {
    val Mercury, Venus, Earth, Mars, Krypton = Value
  }

  // Uses default Jackson serialization format for scala enumerations
  final case class Alien(name: String, plant: Planet.Value)
      extends JsonSerializable

  // Serializes planet values as a JsonString
  class PlanetType extends TypeReference[Planet.type]
  final case class Superhero(
      name: String,
      @JsonScalaEnumeration(classOf[PlanetType]) planet: Planet.Value
  ) extends JsonSerializable
}

final class EnumerationSerializeSpec
    extends TestKit(ActorSystem("enumeration-serialize-test"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {
  import EnumerationSerializeSpec._

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Serialize/Deserialize enumerations" in {
    val actor = system.actorOf(TestActors.echoActorProps)

    actor ! Alien("the alien", Planet.Mars)
    expectMsg(Alien("the alien", Planet.Mars))

    actor ! Superhero("the hero", Planet.Mercury)
    expectMsg(Superhero("the hero", Planet.Mercury))
  }

}
