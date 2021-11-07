package ddata

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.ddata.{GCounter, GCounterKey, SelfUniqueAddress}
import akka.cluster.ddata.typed.scaladsl.{DistributedData, Replicator}

object Counter {
  sealed trait Command
  case object Increment extends Command
  final case class GetValue(replyTo: ActorRef[Int]) extends Command
  final case class GetCachedValue(replyTo: ActorRef[Int]) extends Command
  case object Unsubscribe extends Command

  private sealed trait InternalCommand extends Command
  private case class InternalUpdateResponse(
      response: Replicator.UpdateResponse[GCounter]
  ) extends InternalCommand
  private case class InternalGetResponse(
      response: Replicator.GetResponse[GCounter],
      replyTo: ActorRef[Int]
  ) extends InternalCommand
  private case class InternalSubscribeResponse(
      response: Replicator.SubscribeResponse[GCounter]
  ) extends InternalCommand

  def apply(key: GCounterKey): Behavior[Command] =
    Behaviors.setup { context =>
      implicit val node: SelfUniqueAddress =
        DistributedData(context.system).selfUniqueAddress
      DistributedData.withReplicatorMessageAdapter[Command, GCounter] {
        replicatorAdapter =>
          replicatorAdapter.subscribe(key, InternalSubscribeResponse.apply)
          replicatorAdapter.askUpdate(
            Replicator.Update(key, GCounter.empty, Replicator.WriteLocal)(
              identity
            ),
            InternalUpdateResponse
          )
          def updated(cachedValue: Int): Behavior[Command] =
            Behaviors.receiveMessage {
              case Increment =>
                replicatorAdapter.askUpdate(
                  Replicator.Update(key, GCounter.empty, Replicator.WriteLocal)(
                    _ :+ 1
                  ),
                  InternalUpdateResponse
                )
                Behaviors.same

              case GetValue(replyTo) =>
                replicatorAdapter.askGet(
                  Replicator.Get(key, Replicator.ReadLocal),
                  InternalGetResponse(_, replyTo)
                )
                Behaviors.same

              case GetCachedValue(replyTo) =>
                replyTo ! cachedValue
                Behaviors.same

              case Unsubscribe =>
                replicatorAdapter.unsubscribe(key)
                Behaviors.same

              case internal: InternalCommand =>
                internal match {
                  case InternalUpdateResponse(_) =>
                    Behaviors.same
                  case InternalGetResponse(
                        response @ Replicator.GetSuccess(`key`),
                        replyTo
                      ) =>
                    replyTo ! response.get(key).value.toInt
                    Behaviors.same
                  case InternalGetResponse(_, _) =>
                    // Not dealing with failures
                    Behaviors.unhandled
                  case InternalSubscribeResponse(response) =>
                    response match {
                      case changed @ Replicator.Changed(`key`) =>
                        val value = changed.get(key).value.intValue
                        updated(value)
                      case Replicator.Changed(_) =>
                        Behaviors.unhandled
                      case Replicator.Deleted(_) =>
                        Behaviors.unhandled
                      case akka.cluster.ddata.Replicator.Changed(_) =>
                        Behaviors.unhandled
                      case akka.cluster.ddata.Replicator.Deleted(_) =>
                        Behaviors.unhandled
                    }
                }
            }
          updated(0)
      }
    }
}
