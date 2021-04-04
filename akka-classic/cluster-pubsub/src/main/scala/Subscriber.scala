import akka.actor.{Actor, ActorLogging, Props, Timers}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}

import scala.concurrent.duration._

object Subscriber {
  def props(topic: String, timeout: FiniteDuration = 3.seconds): Props =
    Props(new Subscriber(topic, timeout))
}
class Subscriber(topic: String, timeout: FiniteDuration)
    extends Actor
    with ActorLogging
    with Timers {
  import DistributedPubSubMediator.{Subscribe, SubscribeAck}

  private val mediator = DistributedPubSub(context.system).mediator

  private final object SubscribeDeadlineTimer
  private final object SubscribeDeadlineLetter
  timers.startSingleTimer(
    SubscribeDeadlineTimer,
    SubscribeDeadlineLetter,
    timeout
  )

  log.info("Subscriber: {}", self)

  override def preStart(): Unit = {
    super.preStart()
    mediator ! Subscribe(topic, None, self)
  }

  override def receive: Receive = {
    case SubscribeAck(Subscribe(`topic`, None, self)) =>
      log.info("SubscribeAck: topic={}", topic)
      timers.cancel(SubscribeDeadlineTimer)

    case SubscribeDeadlineLetter =>
      log.info("Cannot subscribe topic={}", topic)
      context.stop(self)

    case message: Message =>
      log.info("Got Message {} from {}", message, sender())
  }
}
