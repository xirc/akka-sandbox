import akka.actor.{Actor, ActorLogging, Props, Timers}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}

import scala.concurrent.duration._

object Sender {
  def props(path: String, interval: FiniteDuration = 2.seconds): Props =
    Props(new Sender(path, interval))
}
class Sender(path: String, interval: FiniteDuration)
    extends Actor
    with ActorLogging
    with Timers {
  import DistributedPubSubMediator.Send

  private val mediator = DistributedPubSub(context.system).mediator
  private var counter: Int = 0

  private final object ProduceMessageTimer
  private final object ProduceMessageLetter
  timers.startTimerAtFixedRate(
    ProduceMessageTimer,
    ProduceMessageLetter,
    interval
  )

  override def receive: Receive = {
    case ProduceMessageLetter =>
      self ! Message(counter.toString)
      counter += 1
    case message: Message =>
      log.info("Send Message {} from {}", message, sender())
      mediator ! Send(path = path, msg = message, localAffinity = true)
  }
}
