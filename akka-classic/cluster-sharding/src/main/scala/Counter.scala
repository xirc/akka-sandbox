import akka.actor.{Actor, ActorLogging, Props, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.HashCodeMessageExtractor

import scala.concurrent.duration._

object Counter {
  def props: Props = Props(new Counter)

  // Commands
  sealed trait Command {
    def id: Long
  }
  final case class Increment(id: Long) extends Command
  final case class Decrement(id: Long) extends Command
  final case class Get(id: Long) extends Command

  // Passivation
  private final case object Stop

  // Event
  final case class CounterChanged(delta: Int)

  // Sharding
  val shardName = "counter-sharding"
  val numberOfShards = 100
  val extractor = new HashCodeMessageExtractor(100) {
    override def entityId(message: Any): String = message match {
      case msg: Command => msg.id.toString
      case _            => null // unhandled
    }
  }
}

class Counter extends Actor with ActorLogging {
  import Counter._
  import ShardRegion.Passivate

  private val id: Long = self.path.name.toLong
  private var count: Int = 0
  private def updateState(event: CounterChanged): Unit = {
    count += event.delta
  }

  context.setReceiveTimeout(120.seconds)

  override def receive: Receive = {
    case Increment(`id`) =>
      log.info("Increment from {}", sender())
      updateState(CounterChanged(+1))
    case Decrement(`id`) =>
      log.info("Decrement from {}", sender())
      updateState(CounterChanged(-1))
    case Get(`id`) =>
      log.info("Get from {}", sender())
      sender() ! count
    case ReceiveTimeout =>
      log.info("Passivate {}", self)
      context.parent ! Passivate(stopMessage = Stop)
    case Stop =>
      log.info("Stop {}", self)
      context.stop(self)
  }
}
