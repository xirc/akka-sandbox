package chatroom.functional

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import akka.NotUsed
import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}

object ChatRoom {
  // --

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

  // --

  private final case class PublishSessionMessage(
      screenName: String,
      message: String
  ) extends RoomCommand
  private final case class NotifyClient(message: MessagePosted)
      extends SessionCommand

  def apply(): Behavior[RoomCommand] = {
    chatRoom(Nil)
  }

  private def chatRoom(
      sessions: List[ActorRef[SessionCommand]]
  ): Behavior[RoomCommand] = {
    Behaviors.receive { (context, message) =>
      message match {
        case GetSession(screenName, client) =>
          val ses = context.spawn(
            session(context.self, screenName, client),
            name = URLEncoder.encode(screenName, StandardCharsets.UTF_8)
          )
          client ! SessionGranted(ses)
          chatRoom(ses :: sessions)
        case PublishSessionMessage(screenName, message) =>
          val notification = NotifyClient(MessagePosted(screenName, message))
          sessions.foreach(_ ! notification)
          Behaviors.same
      }
    }
  }
  private def session(
      room: ActorRef[PublishSessionMessage],
      screenName: String,
      client: ActorRef[SessionEvent]
  ): Behavior[SessionCommand] = {
    Behaviors.receiveMessage {
      case PostMessage(message) =>
        room ! PublishSessionMessage(screenName, message)
        Behaviors.same
      case NotifyClient(message) =>
        client ! message
        Behaviors.same
    }
  }
}

object Gabbler {
  import ChatRoom._

  def apply(): Behavior[SessionEvent] = {
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case SessionGranted(session) =>
          session ! PostMessage("Hello World!")
          Behaviors.same
        case SessionDenied(reason) =>
          context.log.info("cannot start chat room session: {}", reason)
          Behaviors.stopped
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

object Main extends App {
  def apply(): Behavior[NotUsed] = {
    Behaviors.setup { context =>
      val chatRoom = context.spawn(ChatRoom(), "chatroom")
      val gabbler = context.spawn(Gabbler(), "gabbler")
      context.watch(gabbler)
      chatRoom ! ChatRoom.GetSession("ol' Gabbler", gabbler)
      Behaviors.receiveSignal { case (_, Terminated(_)) =>
        Behaviors.stopped
      }
    }
  }

  ActorSystem(Main(), "ChatRoomDemo")
}
