package ddata

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ddata.{LWWRegister, SelfUniqueAddress}
import akka.cluster.ddata.typed.scaladsl.DistributedData

object LWWRegisterExample extends App {
  implicit val system: ActorSystem[Nothing] =
    ActorSystem(Behaviors.empty, "system")
  implicit val node: SelfUniqueAddress = DistributedData(
    system
  ).selfUniqueAddress

  val r1 = LWWRegister.create("Hello")
  val r2 = r1.withValueOf("Hi")
  println(r2.value)
  println(r2.updatedBy)
  println(r2.timestamp)

  system.terminate()
}
