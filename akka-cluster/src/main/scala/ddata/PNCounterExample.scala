package ddata

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ddata.{PNCounter, SelfUniqueAddress}
import akka.cluster.ddata.typed.scaladsl.DistributedData

object PNCounterExample extends App {
  implicit val system: ActorSystem[Nothing] =
    ActorSystem(Behaviors.empty, "system")
  implicit val node: SelfUniqueAddress = DistributedData(
    system
  ).selfUniqueAddress

  val c0 = PNCounter.empty
  val c1 = c0 :+ 1
  val c2 = c1 :+ 7
  val c3 = c2.decrement(2)

  println(c3.value)

  system.terminate()
}
