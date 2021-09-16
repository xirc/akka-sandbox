package sample

import akka.actor.{ActorIdentity, Identify}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import akka.pattern.ask
import akka.testkit.DefaultTimeout
import scala.concurrent.duration._

final class TransformationSpecMultiJvmNode1 extends TransformationSpec
final class TransformationSpecMultiJvmNode2 extends TransformationSpec
final class TransformationSpecMultiJvmNode3 extends TransformationSpec

class TransformationSpec extends TransformationSpecBase with DefaultTimeout {
  import Messages._
  import TransformationSpecConfig._

  "setup a cluster" in within(3.seconds) {
    Cluster(system).subscribe(self, classOf[MemberUp])
    expectMsgType[CurrentClusterState]

    val seedAddress = node(seed).address
    val frontendAddress = node(frontend).address
    val backendAddress = node(backend).address

    Cluster(system).join(seedAddress)
    receiveN(3).collect { case MemberUp(member) =>
      member.address
    }.toSet should be(
      Set(seedAddress, frontendAddress, backendAddress)
    )
    Cluster(system).unsubscribe(self)

    enterBarrier("wait to form a cluster")

    runOn(frontend) {
      system.actorOf(TransformationFrontend.props, Paths.frontend)
    }
    runOn(backend) {
      system.actorOf(TransformationBackend.props, Paths.backend)
    }

    val selection =
      system.actorSelection(node(frontend) / "user" / Paths.frontend)
    awaitCond(
      {
        selection ! GetStatus
        expectMsgType[GetStatusResponse] == Ready
      },
      3.seconds
    )

    enterBarrier("wait to setup actors")
  }

  "execute a transformation job" in within(3.seconds) {
    runOn(seed) {
      val selection =
        system.actorSelection(node(frontend) / "user" / Paths.frontend)
      selection ! TransformationJob("The Upper Case")
      val result = expectMsgType[TransformationResult]
      result.text should be("THE UPPER CASE")
    }
    enterBarrier("wait to execute the job")
  }
}
