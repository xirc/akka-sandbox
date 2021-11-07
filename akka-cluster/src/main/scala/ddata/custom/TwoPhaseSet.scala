package ddata.custom

import akka.cluster.ddata.{
  DeltaReplicatedData,
  GSet,
  ReplicatedData,
  ReplicatedDelta
}

@SerialVersionUID(1L)
final case class TwoPhaseSet[A] private[custom] (
    private[custom] val adds: GSet[A] = GSet.empty[A],
    private[custom] val removals: GSet[A] = GSet.empty[A]
) extends ReplicatedData
    with ReplicatedDelta
    with DeltaReplicatedData {

  override type T = TwoPhaseSet[A]
  override type D = TwoPhaseSet[A]

  def add(element: A): TwoPhaseSet[A] = {
    copy(adds = this.adds.add(element))
  }

  def remove(element: A): TwoPhaseSet[A] = {
    copy(removals = this.removals.add(element))
  }

  def elements: Set[A] = {
    adds.elements.diff(removals.elements)
  }

  override def merge(that: TwoPhaseSet[A]): TwoPhaseSet[A] = {
    copy(
      adds = this.adds.merge(that.adds),
      removals = this.removals.merge(that.removals)
    )
  }

  override def zero: DeltaReplicatedData = TwoPhaseSet.empty[A]

  override def delta: Option[TwoPhaseSet[A]] = {
    val addsDelta = adds.delta.getOrElse(GSet.empty)
    val removalsDelta = removals.delta.getOrElse(GSet.empty)
    Some(TwoPhaseSet(addsDelta, removalsDelta))
  }

  override def mergeDelta(thatDelta: TwoPhaseSet[A]): TwoPhaseSet[A] = {
    val adds = this.adds.mergeDelta(thatDelta.adds)
    val removals = this.removals.mergeDelta(thatDelta.removals)
    TwoPhaseSet(adds, removals)
  }

  override def resetDelta: TwoPhaseSet[A] = {
    TwoPhaseSet(adds.resetDelta, removals.resetDelta)
  }
}

object TwoPhaseSet {
  def empty[A]: TwoPhaseSet[A] = TwoPhaseSet[A]()
}
