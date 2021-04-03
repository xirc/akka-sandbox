package chatroom.oop

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import akka.NotUsed
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import akka.actor.typed.scaladsl.{
  AbstractBehavior,
  ActorContext,
  Behaviors,
  LoggerOps
}

object ChatRoom {
  sealed trait RoomCommand
  final case class GetSession(
      screenName: String,
      replyTo: ActorRef[SessionEvent]
  ) extends RoomCommand

  sealed trait SessionEvent
  final case class SessionGranted(session: ActorRef[PostMessage])
      extends SessionEvent
  final case class SessionDenied(reason: String) extends SessionEvent
  final case class MessagePosted(screenName: String, message: String)
      extends SessionEvent

  sealed trait SessionCommand
  final case class PostMessage(message: String) extends SessionCommand

  private final case class NotifyClient(message: MessagePosted)
      extends SessionCommand
  private final case class PublishSessionMessage(
      screenName: String,
      message: String
  ) extends RoomCommand

  def apply(): Behavior[RoomCommand] = {
    Behaviors.setup(context => new ChatRoomBehavior(context))
  }

  private final class ChatRoomBehavior(context: ActorContext[RoomCommand])
      extends AbstractBehavior[RoomCommand](context) {
    private var sessions: List[ActorRef[SessionCommand]] = Nil

    override def onMessage(msg: RoomCommand): Behavior[RoomCommand] = {
      msg match {
        case GetSession(screenName, client) =>
          val ses = context.spawn(
            SessionBehavior(context.self, screenName, client),
            name = URLEncoder.encode(screenName, StandardCharsets.UTF_8)
          )
          client ! SessionGranted(ses)
          sessions = ses :: sessions
          this
        case PublishSessionMessage(screenName, message) =>
          val notification = NotifyClient(MessagePosted(screenName, message))
          sessions.foreach(_ ! notification)
          this
      }
    }
  }

  object SessionBehavior {
    def apply(
        room: ActorRef[PublishSessionMessage],
        screenName: String,
        client: ActorRef[SessionEvent]
    ): Behavior[SessionCommand] = {
      Behaviors.setup(context =>
        new SessionBehavior(context, room, screenName, client)
      )
    }
  }
  private final class SessionBehavior(
      context: ActorContext[SessionCommand],
      room: ActorRef[PublishSessionMessage],
      screenName: String,
      client: ActorRef[SessionEvent]
  ) extends AbstractBehavior[SessionCommand](context) {
    override def onMessage(msg: SessionCommand): Behavior[SessionCommand] = {
      msg match {
        case PostMessage(message) =>
          room ! PublishSessionMessage(screenName, message)
          Behaviors.same
        case NotifyClient(message) =>
          client ! message
          Behaviors.same
      }
    }
  }

}

object Gabbler {
  import ChatRoom._

  def apply(): Behavior[SessionEvent] = {
    Behaviors.setup { context =>
      new AbstractBehavior[SessionEvent](context) {
        override def onMessage(msg: SessionEvent): Behavior[SessionEvent] = {
          msg match {
            case SessionDenied(reason) =>
              context.log.info("cannot start chat room session: {}", reason)
              Behaviors.stopped
            case SessionGranted(session) =>
              session ! PostMessage("Hello World!")
              Behaviors.same
            case MessagePosted(screenName, message) =>
              context.log.info2(
                "message has been posted by '{}': {}",
                screenName,
                message
              )
              Behaviors.stopped
          }
        }
      }
    }
  }
}

object Main extends App {
  def apply(): Behavior[NotUsed] =
    Behaviors.setup { context =>
      val chatRoom = context.spawn(ChatRoom(), "chatroom")
      val gabbler = context.spawn(Gabbler(), "gabbler")
      context.watch(gabbler)
      chatRoom ! ChatRoom.GetSession("ol' Gabbler", gabbler)
      Behaviors.receiveSignal { case (_, Terminated(_)) =>
        Behaviors.stopped
      }
    }

  ActorSystem(Main(), "ChatRoomDemo")
}
