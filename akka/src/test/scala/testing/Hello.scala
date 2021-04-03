package testing

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object Hello {
  sealed trait Command
  case object CreateAnonymousChild extends Command
  case class CreateChild(childName: String) extends Command
  case class SayHelloToChild(childName: String) extends Command
  case object SayHelloToAnonymousChild extends Command
  case class SayHello(who: ActorRef[String]) extends Command
  case class LogAndSayHello(who: ActorRef[String]) extends Command

  val childActor: Behavior[String] = {
    Behaviors.ignore
  }

  def apply(): Behavior[Command] = {
    Behaviors.receive {
      case (context, CreateChild(name)) =>
        context.spawn(childActor, name)
        Behaviors.same
      case (context, CreateAnonymousChild) =>
        context.spawnAnonymous(childActor)
        Behaviors.same
      case (context, SayHelloToChild(childName)) =>
        val child = context.spawn(childActor, childName)
        child ! "hello"
        Behaviors.same
      case (context, SayHelloToAnonymousChild) =>
        val child = context.spawnAnonymous(childActor)
        child ! "hello stranger"
        Behaviors.same
      case (_, SayHello(who)) =>
        who ! "hello"
        Behaviors.same
      case (context, LogAndSayHello(who)) =>
        context.log.info("Saying hello to {}", who.path.name)
        who ! "hello"
        Behaviors.same
    }
  }
}
