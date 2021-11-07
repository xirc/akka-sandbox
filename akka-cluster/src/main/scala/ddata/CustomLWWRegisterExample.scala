package ddata

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ddata.{LWWRegister, SelfUniqueAddress}
import akka.cluster.ddata.typed.scaladsl.DistributedData

object CustomLWWRegisterExample extends App {
  implicit val system: ActorSystem[Nothing] =
    ActorSystem(Behaviors.empty, "system")
  implicit val node: SelfUniqueAddress = DistributedData(
    system
  ).selfUniqueAddress

  case class Record(version: Int, name: String, address: String)
  implicit val clock = new LWWRegister.Clock[Record] {
    override def apply(currentTimestamp: Long, value: Record): Long = {
      value.version
    }
  }

  val record1 = Record(version = 1, "Alice", "Union Square")
  val r1 = LWWRegister(node, record1, clock)

  val record2 = Record(version = 2, "Alice", "Madison Square")
  val r2 = LWWRegister(node, record2, clock)

  val r3 = r1.merge(r2)
  println(r3.value)
  println(r3.updatedBy)
  println(r3.timestamp)

  system.terminate()
}
