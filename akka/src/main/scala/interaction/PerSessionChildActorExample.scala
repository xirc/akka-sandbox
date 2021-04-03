package interaction

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

object PerSessionChildActorExample extends App {

  case class Keys(owner: String)

  case class Wallet(owner: String)

  object KeyCabinet {

    final case class GetKeys(who: String, replyTo: ActorRef[Keys])

    def apply(): Behavior[GetKeys] = {
      Behaviors.receiveMessage { message =>
        message.replyTo ! Keys(message.who)
        Behaviors.same
      }
    }
  }

  object Drawer {

    final case class GetWallet(who: String, replyTo: ActorRef[Wallet])

    def apply(): Behavior[GetWallet] = {
      Behaviors.receiveMessage { message =>
        message.replyTo ! Wallet(message.who)
        Behaviors.same
      }
    }
  }

  object Home {

    sealed trait Command

    final case class LeaveHome(who: String, replyTo: ActorRef[ReadyToLeaveHome]) extends Command

    final case class ReadyToLeaveHome(who: String, keys: Keys, wallet: Wallet)

    def apply(): Behavior[Command] = {
      Behaviors.setup { context =>
        val keyCabinet = context.spawn(KeyCabinet(), "key-cabinet")
        val drawer = context.spawn(Drawer(), "drawer")
        Behaviors.receiveMessage {
          case LeaveHome(who, replyTo) =>
            context.spawn(prepareToLeaveHome(who, replyTo, keyCabinet, drawer), s"leaving-$who")
            Behaviors.same
        }
      }
    }

    private def prepareToLeaveHome
    (
      whoIsLeaving: String,
      replyTo: ActorRef[ReadyToLeaveHome],
      keyCabinet: ActorRef[KeyCabinet.GetKeys],
      drawer: ActorRef[Drawer.GetWallet]
    ): Behavior[NotUsed] = {
      Behaviors.setup[AnyRef] { context =>
        var walletOpt: Option[Wallet] = None
        var keysOpt: Option[Keys] = None

        keyCabinet ! KeyCabinet.GetKeys(whoIsLeaving, context.self.narrow[Keys])
        drawer ! Drawer.GetWallet(whoIsLeaving, context.self.narrow[Wallet])

        def nextBehavior(): Behavior[AnyRef] = {
          (keysOpt, walletOpt) match {
            case (Some(k), Some(w)) =>
              replyTo ! ReadyToLeaveHome(whoIsLeaving, k, w)
              Behaviors.stopped
            case _ =>
              Behaviors.same
          }
        }

        Behaviors.receiveMessage {
          case w: Wallet =>
            walletOpt = Some(w)
            nextBehavior()
          case k: Keys =>
            keysOpt = Some(k)
            nextBehavior()
          case _ =>
            Behaviors.unhandled
        }
      }.narrow[NotUsed]
    }
  }

  object Main {
    def apply(): Behavior[NotUsed] = {
      Behaviors.setup { context =>
        val home = context.spawn(Home(), "home")
        val client = context.spawn(
          Behaviors.logMessages(Behaviors.ignore[Home.ReadyToLeaveHome]), "client"
        )
        home ! Home.LeaveHome("alice", client)
        Behaviors.same
      }
    }
  }

  val system = ActorSystem(Main(), "system")
  Thread.sleep(100)
  system.terminate()
}
