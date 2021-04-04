package example

import akka.actor.ActorSystem
import akka.serialization.{SerializationExtension, Serializers}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class ProgrammaticSerializeSpec
    extends TestKit(ActorSystem("programmatic-serialize-test"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {
  val serialization = SerializationExtension(system)

  private def serializeAndThenDeserialize[T <: AnyRef](
      originalMessage: T
  ): T = {
    val serializer = serialization.serializerFor(originalMessage.getClass)

    // Serialize
    // (bytes, serialize id, manifest) should always be transferred or stored together,
    // so that they can be deserialized with different 'serialization-bindings' configuration.
    val bytes = serializer.toBinary(originalMessage)
    val serializerId = serializer.identifier
    val manifest = Serializers.manifestFor(serializer, originalMessage)

    // Deserialize
    val backMessage =
      serialization.deserialize(bytes, serializerId, manifest).get
    backMessage.asInstanceOf[T]
  }

  "JsonMessage can be serialize and deserialize" in {
    val originalMessage = Messages.JsonMessage
    val backMessage = serializeAndThenDeserialize(originalMessage)
    originalMessage shouldBe backMessage
  }

  "CborMessage can be serialize and deserialize" in {
    val originalMessage = Messages.CborMessage
    val backMessage = serializeAndThenDeserialize(originalMessage)
    originalMessage shouldBe backMessage
  }
}
