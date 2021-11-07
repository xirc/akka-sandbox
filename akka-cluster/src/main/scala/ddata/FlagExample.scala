package ddata

import akka.cluster.ddata.Flag

object FlagExample extends App {
  val f0 = Flag.Disabled
  val f1 = f0.switchOn
  println(f1.enabled)
}
