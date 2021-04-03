package example

import java.nio.charset.StandardCharsets

import akka.serialization.Serializer

final class MyOwnSerializer extends Serializer {
  private val utf8 = StandardCharsets.UTF_8
  private val MyOwnMessageManifest = classOf[Messages.MyOwnMessage]

  override def identifier: Int = 1234567

  override def includeManifest: Boolean = true

  override def toBinary(o: AnyRef): Array[Byte] = {
    o match {
      case Messages.MyOwnMessage(value) =>
        value.getBytes(utf8)
      case _ =>
        throw new IllegalArgumentException()
    }
  }

  override def fromBinary(
      bytes: Array[Byte],
      manifest: Option[Class[_]]
  ): AnyRef = {
    manifest match {
      case Some(`MyOwnMessageManifest`) =>
        Messages.MyOwnMessage(new String(bytes, utf8))
      case _ =>
        throw new IllegalArgumentException()
    }
  }
}
