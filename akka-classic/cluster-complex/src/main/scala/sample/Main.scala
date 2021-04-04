package sample

import akka.actor.ActorSystem
import akka.cluster.Cluster

object Main extends App {
  val system = ActorSystem("ClusterSystem")

  val cluster = Cluster(system)
  cluster.registerOnMemberUp {
    system.log.info("registerOnMemberUp")
    val selfMember = cluster.selfMember
    val hasBackendRole = selfMember.hasRole(Roles.backend)
    val hasFrontendRole = selfMember.hasRole(Roles.frontend)
    if (hasBackendRole) {
      system.actorOf(TransformationBackend.props, Paths.backend)
    }
    if (hasFrontendRole) {
      system.actorOf(TransformationFrontend.props, Paths.frontend)
    }
  }
  cluster.registerOnMemberRemoved {
    system.log.info("registerOnMemberRemoved")
  }
}
