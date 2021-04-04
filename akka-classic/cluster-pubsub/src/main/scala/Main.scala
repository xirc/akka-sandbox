import akka.actor.ActorSystem
import akka.cluster.Cluster

object Main extends App {
  val system = ActorSystem("PubSubSystem")
  val cluster = Cluster(system)

  cluster.registerOnMemberUp {
    val selfMember = cluster.selfMember
    val hasPublisherRole = selfMember.hasRole("publisher")
    val hasSubscriberRole = selfMember.hasRole("subscriber")
    val topic = system.settings.config.getString("application.topic")
    if (hasPublisherRole) {
      system.log.info("Publisher: {}", topic)
      system.actorOf(Publisher.props(topic))
    }
    if (hasSubscriberRole) {
      system.log.info("Subscriber: {}", topic)
      system.actorOf(Subscriber.props(topic))
    }
  }
}
