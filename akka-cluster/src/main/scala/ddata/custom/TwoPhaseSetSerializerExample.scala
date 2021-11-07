package ddata.custom

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.serialization.{SerializationExtension, Serializers}

object TwoPhaseSetSerializerExample extends App {
  val system = ActorSystem(Behaviors.empty, "system")
  val serialization = SerializationExtension(system)

  private def test[T](original: TwoPhaseSet[T]): Unit = {
    val bytes = serialization.serialize(original).get
    val serializer = serialization.findSerializerFor(original)
    val serializerId = serializer.identifier
    val manifest = Serializers.manifestFor(serializer, original)
    val deserialized = serialization
      .deserialize(bytes, serializerId, manifest)
      .get
      .asInstanceOf[TwoPhaseSet[T]]
    println(s"$original -> $deserialized")
  }

  val s0 = TwoPhaseSet.empty[Int]
  val s1 = s0.add(1)
  val s2 = s1.add(2)
  val s3 = s2.remove(1)

  test(s0)
  test(s1)
  test(s2)
  test(s3)

  system.terminate()
}
