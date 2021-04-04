import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}

import scala.concurrent.duration._

object Client {
  def props(
      id: Long,
      delta: Int,
      counter: ActorRef,
      interval: FiniteDuration = 1.seconds
  ): Props =
    Props(new Client(id, delta, counter, interval))
}
class Client(id: Long, delta: Int, counter: ActorRef, interval: FiniteDuration)
    extends Actor
    with ActorLogging
    with Timers {
  final object IncrementTimerKey
  final object IncrementTimerLetter
  timers.startTimerAtFixedRate(
    IncrementTimerKey,
    IncrementTimerLetter,
    interval
  )

  override def receive: Receive = {
    case IncrementTimerLetter if delta > 0 =>
      (1 to delta) foreach { _ =>
        counter ! Counter.Increment(id)
      }
    case IncrementTimerLetter if delta < 0 =>
      (1 to delta.abs) foreach { _ =>
        counter ! Counter.Decrement(id)
      }
  }
}
