package example

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

final class SerializableSpec
    extends TestKit(ActorSystem("serializable-test"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

  "actor cannot send messages with JavaSerialization" in {
    val actor = system.actorOf(TestActors.echoActorProps)
    actor ! Messages.JavaMessage
    expectNoMessage()
  }

  "actor can send messages with JsonSerialization" in {
    val actor = system.actorOf(TestActors.echoActorProps)
    actor ! Messages.JsonMessage
    expectMsg(Messages.JsonMessage)
  }

  "actor can send messages with CborSerialization" in {
    val actor = system.actorOf(TestActors.echoActorProps)
    actor ! Messages.CborMessage
    expectMsg(Messages.CborMessage)
  }

  "actor can send messages with MyOwnSerialization" in {
    val actor = system.actorOf(TestActors.echoActorProps)
    actor ! Messages.MyOwnMessage("abc")
    expectMsg(Messages.MyOwnMessage("abc"))
  }

  "actor can send messages with MyOwnSerialization2" in {
    val actor = system.actorOf(TestActors.echoActorProps)
    actor ! Messages.MyOwnMessage2("def")
    expectMsg(Messages.MyOwnMessage2("def"))
  }
}
