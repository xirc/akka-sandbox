package example

object Messages {
  final object JavaMessage
  final object JsonMessage extends JsonSerializable
  final object CborMessage extends CborSerializable

  final case class MyOwnMessage(value: String) extends MyOwnSerializable
  final case class MyOwnMessage2(value: String) extends MyOwnSerializable2
}
