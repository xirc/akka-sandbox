package example

import java.nio.charset.StandardCharsets

import akka.serialization.SerializerWithStringManifest

final class MyOwnSerializer2 extends SerializerWithStringManifest {
  private val utf8 = StandardCharsets.UTF_8
  private object Manifests {
    val MyOwnMessage2 = "my-own-message2"
  }

  override def identifier: Int = 7654321

  override def manifest(o: AnyRef): String = {
    o match {
      case _: Messages.MyOwnMessage2 =>
        Manifests.MyOwnMessage2
      case _ =>
        throw new IllegalArgumentException()
    }
  }

  override def toBinary(o: AnyRef): Array[Byte] = {
    o match {
      case Messages.MyOwnMessage2(value) =>
        value.getBytes(utf8)
      case _ =>
        throw new IllegalArgumentException()
    }
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    manifest match {
      case Manifests.MyOwnMessage2 =>
        Messages.MyOwnMessage2(new String(bytes, utf8))
      case _ =>
        throw new IllegalArgumentException()
    }
  }
}
