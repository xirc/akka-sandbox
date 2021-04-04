import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}

object Destination {
  def props: Props = Props(new Destination)
}

class Destination extends Actor with ActorLogging {
  import DistributedPubSubMediator.Put

  private val mediator = DistributedPubSub(context.system).mediator

  mediator ! Put(self)

  log.info("Destination {}", self)

  override def receive: Receive = { case message: Message =>
    log.info("Got Message {} from {}", message, sender())
  }
}
