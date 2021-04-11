package discovery

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors

object ReceptionistExample extends App {

  object PingService {
    val PingServiceKey = ServiceKey[Ping]("ping-service")
    final case class Ping(replyTo: ActorRef[Pong.type])
    case object Pong

    def apply(): Behavior[Ping] = {
      Behaviors.setup { context =>
        context.system.receptionist ! Receptionist.Register(
          PingServiceKey,
          context.self
        )
        Behaviors.receiveMessage { case Ping(replyTo) =>
          context.log.info("Pinged by {}", replyTo)
          replyTo ! Pong
          Behaviors.same
        }
      }
    }
  }

  object Pinger {
    def apply(
        pingService: ActorRef[PingService.Ping]
    ): Behavior[PingService.Pong.type] = {
      Behaviors.setup { context =>
        pingService ! PingService.Ping(context.self)
        Behaviors.receiveMessage { _ =>
          context.log.info("{} was ponged!!", context.self)
          Behaviors.stopped
        }
      }
    }
  }

  object Guardian {
    def apply(): Behavior[Nothing] = {
      Behaviors
        .setup[Receptionist.Listing] { context =>
          context.spawnAnonymous(PingService())
          context.system.receptionist !
            Receptionist.Subscribe(PingService.PingServiceKey, context.self)
          Behaviors.receiveMessagePartial[Receptionist.Listing] {
            case PingService.PingServiceKey.Listing(listings) =>
              listings.foreach { ps =>
                context.spawnAnonymous(Pinger(ps))
              }
              Behaviors.same
          }
        }
        .narrow
    }
  }

  object PingManager {
    sealed trait Command
    case object PingAll extends Command
    private case class ListingResponse(listing: Receptionist.Listing)
        extends Command

    def apply(): Behavior[Command] = {
      Behaviors.setup { context =>
        val adapter =
          context.messageAdapter[Receptionist.Listing](ListingResponse)
        context.spawnAnonymous(PingService())
        Behaviors.receiveMessagePartial {
          case PingAll =>
            context.system.receptionist !
              Receptionist.Find(PingService.PingServiceKey, adapter)
            Behaviors.same
          case ListingResponse(PingService.PingServiceKey.Listing(listing)) =>
            listing.foreach { ps =>
              context.spawnAnonymous(Pinger(ps))
            }
            Behaviors.same
        }
      }
    }
  }

  val guardian = ActorSystem[Nothing](Guardian(), "guardian")
  Thread.sleep(100)
  guardian.terminate()

  val manager = ActorSystem(PingManager(), "manager")
  manager ! PingManager.PingAll
  Thread.sleep(100)
  manager.terminate()
}
