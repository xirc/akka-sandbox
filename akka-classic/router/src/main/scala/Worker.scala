import akka.actor.{Actor, ActorLogging, Props}

import scala.concurrent.duration.FiniteDuration

object Worker {
  def props: Props = Props(new Worker)
  def props(processingTime: FiniteDuration): Props = Props(
    new Worker(Some(processingTime))
  )
}
final class Worker private (processingTimeOpt: Option[FiniteDuration] = None)
    extends Actor
    with ActorLogging {
  override def receive: Receive = { case msg =>
    processingTimeOpt.foreach(time => {
      Thread.sleep(time.toMillis)
    })
    log.info("{}", msg)
    sender() ! msg
  }
}
