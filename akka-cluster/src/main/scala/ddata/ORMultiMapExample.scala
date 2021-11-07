package ddata

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ddata.{DistributedData, ORMultiMap, SelfUniqueAddress}

object ORMultiMapExample extends App {
  implicit val system: ActorSystem[Nothing] =
    ActorSystem(Behaviors.empty, "system")
  implicit val node: SelfUniqueAddress = DistributedData(
    system
  ).selfUniqueAddress

  val m0 = ORMultiMap.empty[String, Int]
  val m1 = m0 :+ ("a" -> Set(1, 2, 3))
  val m2 = m1.addBindingBy("a", 4)
  val m3 = m2.removeBindingBy("a", 2)
  val m4 = m3.addBindingBy("b", 1)

  println(m4.entries)

  system.terminate()
}
