import akka.actor.{Actor, ActorLogging, Props, Timers}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator

import scala.concurrent.duration._

object Publisher {
  def props(topic: String, interval: FiniteDuration = 2.seconds): Props =
    Props(new Publisher(topic, interval))
}
class Publisher(topic: String, interval: FiniteDuration)
    extends Actor
    with ActorLogging
    with Timers {
  import DistributedPubSubMediator.Publish

  private final object ProduceTimerKey
  private final object ProduceTimerLetter
  timers.startTimerAtFixedRate(ProduceTimerKey, ProduceTimerLetter, interval)

  private val mediator = DistributedPubSub(context.system).mediator
  private var counter: Int = 0

  log.info("Publisher: {}", self)

  override def receive: Receive = {
    case ProduceTimerLetter =>
      self ! Message(counter.toString)
      counter += 1
    case message: Message =>
      log.info("publish message {} from {}", message, sender())
      mediator ! Publish(topic, message)
  }
}
