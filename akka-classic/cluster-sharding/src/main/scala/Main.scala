import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}

object Main extends App {
  val system = ActorSystem("sharding-system")
  val cluster = Cluster(system)

  cluster.registerOnMemberUp {
    val selfMember = cluster.selfMember
    val hasClientRole = selfMember.hasRole("client")
    val hasCounterRole = selfMember.hasRole("counter")
    if (hasClientRole) {
      setupForClientRole()
    }
    if (hasCounterRole) {
      setupForCounterRole()
    }
  }

  def setupForCounterRole(): Unit = {
    system.log.info("Counter Role")
    ClusterSharding(system).start(
      typeName = Counter.shardName,
      entityProps = Counter.props,
      settings = ClusterShardingSettings(system).withRole("counter"),
      extractEntityId = Counter.extractEntityId,
      extractShardId = Counter.extractShardId
    )
  }

  def setupForClientRole(): Unit = {
    system.log.info("Client Role")
    val proxy = ClusterSharding(system).startProxy(
      Counter.shardName,
      Some("counter"),
      Counter.extractEntityId,
      Counter.extractShardId
    )
    val id = system.settings.config.getLong("application.client.id")
    val delta = system.settings.config.getInt("application.client.delta")
    system.actorOf(Client.props(id, delta, proxy))
  }
}
