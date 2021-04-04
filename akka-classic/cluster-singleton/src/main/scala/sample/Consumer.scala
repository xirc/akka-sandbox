package sample

import akka.actor.{Actor, ActorLogging, Props}

object Consumer {
  def props: Props = Props(new Consumer)

  case class Ping(value: Int)
  case class Pong(value: Int)
}
class Consumer extends Actor with ActorLogging {
  import Consumer._

  override def receive: Receive = { case Ping(value) =>
    log.info("Ping({}) from {}", value, sender())
    sender() ! Pong(value)
  }
}
