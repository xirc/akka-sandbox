package ddata

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ddata.{PNCounterMap, SelfUniqueAddress}
import akka.cluster.ddata.typed.scaladsl.DistributedData

object PNCounterMapExample extends App {
  implicit val system: ActorSystem[Nothing] =
    ActorSystem(Behaviors.empty, "system")
  implicit val node: SelfUniqueAddress = DistributedData(
    system
  ).selfUniqueAddress

  val m0 = PNCounterMap.empty[String]
  val m1 = m0.incrementBy("a", 7)
  val m2 = m1.decrementBy("a", 2)
  val m3 = m2.incrementBy("b", 1)

  println(m3.get("a"))
  m3.entries.foreach { case (key, value) =>
    println(s"$key -> $value")
  }

  system.terminate()
}
