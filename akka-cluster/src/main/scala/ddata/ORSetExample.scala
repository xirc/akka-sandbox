package ddata

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ddata.{ORSet, SelfUniqueAddress}
import akka.cluster.ddata.typed.scaladsl.DistributedData

object ORSetExample extends App {
  implicit val system: ActorSystem[Nothing] =
    ActorSystem(Behaviors.empty, "system")
  implicit val node: SelfUniqueAddress = DistributedData(
    system
  ).selfUniqueAddress

  val s0 = ORSet.empty[String]
  val s1 = s0 :+ "a"
  val s2 = s1 :+ "b"
  val s3 = s2.remove("a")

  println(s3.elements)

  system.terminate()
}
