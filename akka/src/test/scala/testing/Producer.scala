package testing

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, Scheduler}
import akka.util.Timeout

import scala.concurrent.Future
import scala.util.Try

object Producer {
  case class Message(i: Int, replyTo: ActorRef[Try[Int]])
}
class Producer(publisher: ActorRef[Producer.Message])(implicit scheduler: Scheduler) {
  def produce(messages: Int)(implicit timeout: Timeout): Unit = {
    (0 until messages).foreach(publish)
  }
  private def publish(i: Int)(implicit timeout: Timeout): Future[Try[Int]] = {
    publisher.ask(Producer.Message(i, _))
  }
}
