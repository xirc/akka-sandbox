package ddata.custom.protobuf

import akka.actor.ExtendedActorSystem
import akka.cluster.ddata.GSet
import akka.cluster.ddata.protobuf.SerializationSupport
import akka.serialization.Serializer
import com.google.protobuf.ByteString
import ddata.custom.TwoPhaseSet

class TwoPhaseSetSerializer(val system: ExtendedActorSystem)
    extends Serializer
    with SerializationSupport {

  override def includeManifest: Boolean = false

  override def identifier = 99999

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case m: TwoPhaseSet[_] => twoPhaseSetToProto(m).toByteArray
    case _ =>
      throw new IllegalArgumentException(
        s"Can't serialize object of type ${o.getClass}"
      )
  }

  override def fromBinary(
      bytes: Array[Byte],
      manifest: Option[Class[_]]
  ): AnyRef = {
    twoPhaseSetFromBinary(bytes)
  }

  private def twoPhaseSetToProto(
      twoPhaseSet: TwoPhaseSet[_]
  ): messages.TwoPhaseSet = {
    messages
      .TwoPhaseSet()
      .withAdds(
        ByteString.copyFrom(otherMessageToProto(twoPhaseSet.adds).toByteArray)
      )
      .withRemovals(
        ByteString.copyFrom(
          otherMessageToProto(twoPhaseSet.removals).toByteArray
        )
      )
  }

  private def twoPhaseSetFromBinary(bytes: Array[Byte]): TwoPhaseSet[_] = {
    val message = messages.TwoPhaseSet.parseFrom(bytes)
    val adds =
      otherMessageFromBinary(message.adds.toByteArray).asInstanceOf[GSet[Any]]
    val removals = otherMessageFromBinary(message.removals.toByteArray)
      .asInstanceOf[GSet[Any]]
    TwoPhaseSet[Any](adds, removals)
  }

}
