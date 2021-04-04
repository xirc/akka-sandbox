import akka.actor.{Actor, Props}

object Workers {
  def props: Props = Props(new Workers(size = 3))
  def props(size: Int): Props = Props(new Workers(size))
  def makePaths(prefix: String, size: Int = 3): IndexedSeq[String] = {
    for (i <- 1 to size) yield s"${prefix}w${i}"
  }
}
final class Workers private (size: Int) extends Actor {
  for (i <- 1 to size) {
    context.actorOf(Worker.props, s"w${i}")
  }
  override def receive: Receive = PartialFunction.empty
}
