package ddata

import ddata.custom.TwoPhaseSet

object TwoPhaseSetExample extends App {
  val s0 = TwoPhaseSet.empty[String]

  val s1 = s0.add("a")
  val s2 = s1.add("b")
  val s3 = s2.add("c")
  println(s3.elements)

  val s4 = s3.remove("b")
  println(s4.elements)

  val s5 = s4.add("b") // No effect
  println(s5.elements)
}
