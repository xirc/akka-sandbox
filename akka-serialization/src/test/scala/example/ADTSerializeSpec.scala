package example

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{
  DeserializationContext,
  SerializerProvider
}
import com.fasterxml.jackson.databind.annotation.{
  JsonDeserialize,
  JsonSerialize
}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import example.ADTSerializeSpec.{Compass, Direction}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

object ADTSerializeSpec {
  @JsonSerialize(`using` = classOf[DirectionJsonSerializer])
  @JsonDeserialize(`using` = classOf[DirectionJsonDeserializer])
  sealed trait Direction

  object Direction {
    case object North extends Direction
    case object East extends Direction
    case object South extends Direction
    case object West extends Direction
  }

  final class DirectionJsonSerializer
      extends StdSerializer[Direction](classOf[Direction]) {
    import Direction._

    override def serialize(
        value: Direction,
        gen: JsonGenerator,
        provider: SerializerProvider
    ): Unit = {
      val strValue = value match {
        case North => "N"
        case East  => "E"
        case South => "S"
        case West  => "W"
      }
      gen.writeString(strValue)
    }
  }

  final class DirectionJsonDeserializer
      extends StdDeserializer[Direction](classOf[Direction]) {
    import Direction._

    override def deserialize(
        p: JsonParser,
        ctxt: DeserializationContext
    ): Direction = {
      p.getText match {
        case "N" => North
        case "E" => East
        case "S" => South
        case "W" => West
      }
    }
  }

  final case class Compass(currentDirection: Direction) extends JsonSerializable
}

final class ADTSerializeSpec
    extends TestKit(ActorSystem("adt-serialize-test"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "serialize/deserialize ADT" in {
    val actor = system.actorOf(TestActors.echoActorProps)

    actor ! Compass(Direction.North)
    expectMsg(Compass(Direction.North))

    actor ! Compass(Direction.East)
    expectMsg(Compass(Direction.East))

    actor ! Compass(Direction.South)
    expectMsg(Compass(Direction.South))

    actor ! Compass(Direction.West)
    expectMsg(Compass(Direction.West))
  }

}
