package ddata

import akka.cluster.ddata.GSet

object GSetExample extends App {
  val s0 = GSet.empty[String]
  val s1 = s0 + "a"
  val s2 = s1 + "b" + "c"
  if (s2.contains("a")) {
    println(s2.elements)
  }
}
