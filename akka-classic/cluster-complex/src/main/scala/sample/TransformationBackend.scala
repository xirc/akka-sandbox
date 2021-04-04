package sample

import akka.actor.{Actor, ActorLogging, Props, RootActorPath}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import akka.cluster.{Cluster, Member, MemberStatus}

object TransformationBackend {
  def props: Props = Props(new TransformationBackend)
}
class TransformationBackend extends Actor with ActorLogging {
  import Messages._

  private val cluster = Cluster(context.system)

  log.info("backend")

  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberUp])
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  override def receive: Receive = {
    case TransformationJob(text) =>
      log.info("TransformationJob {}", text)
      sender() ! TransformationResult(text.toUpperCase)
    case state: CurrentClusterState =>
      log.info("CurrentClusterState")
      state.members.filter(_.status == MemberStatus.Up).foreach(register)
    case MemberUp(member) =>
      log.info("MemberUp {}", member.address)
      register(member)
  }

  private def register(member: Member): Unit = {
    if (member.hasRole(Roles.frontend)) {
      val selection = context.actorSelection(
        RootActorPath(member.address) / "user" / Paths.frontend
      )
      selection ! BackendRegistration
    }
  }
}
