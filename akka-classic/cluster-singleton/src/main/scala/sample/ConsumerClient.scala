package sample

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}

import scala.concurrent.duration.FiniteDuration

object ConsumerClient {
  def props(consumerRef: ActorRef, interval: FiniteDuration): Props =
    Props(new ConsumerClient(consumerRef, interval))
}
class ConsumerClient(consumerRef: ActorRef, interval: FiniteDuration)
    extends Actor
    with ActorLogging
    with Timers {
  final object TimerKey
  final object TriggerPing
  timers.startTimerAtFixedRate(TimerKey, TriggerPing, interval)

  private var counter: Int = 0

  override def receive: Receive = {
    case TriggerPing =>
      consumerRef ! Consumer.Ping(counter)
      counter += 1
    case Consumer.Pong(value) =>
      log.info("Pong({}) from {}", value, sender())
  }
}
