package example

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

final class BuncherSpec
    extends TestKit(ActorSystem("simple-fsm-spec"))
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with ImplicitSender {
  override def afterAll(): Unit = {
    shutdown(system)
  }

  "batch well" in {
    val buncher = system.actorOf(Props(new Buncher))

    buncher ! SetTarget(self)
    buncher ! Queue(42)
    buncher ! Queue(43)
    expectMsg(Batch(Seq(42, 43)))

    buncher ! Queue(44)
    buncher ! Flush
    buncher ! Queue(45)

    expectMsg(Batch(Seq(44)))
    expectMsg(Batch(Seq(45)))
  }

  "not bach if uninitialized" in {
    val buncher = system.actorOf(Props(new Buncher))
    buncher ! Queue(60)
    expectNoMessage()
  }

}
