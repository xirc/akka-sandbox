import akka.actor.ActorSystem
import akka.cluster.Cluster

object Main extends App {
  val system = ActorSystem("PubSub2P2System")
  val cluster = Cluster(system)

  cluster.registerOnMemberUp {
    val selfMember = cluster.selfMember
    val hasSenderRole = selfMember.hasRole("sender")
    val hasDestinationRole = selfMember.hasRole("destination")
    if (hasSenderRole) {
      system.log.info("Sender")
      system.actorOf(Sender.props("/user/destination-actor"), "sender")
    }
    if (hasDestinationRole) {
      system.log.info("Destination")
      system.actorOf(Destination.props, "destination-actor")
    }
  }
}
