import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.pattern.{ask, pipe}
import akka.testkit
import akka.testkit.{ImplicitSender, TestActor, TestKit, TestProbe}
import akka.util.Timeout
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

import scala.language.reflectiveCalls

class MyMultipleProbeSpec
    extends TestKit(ActorSystem("test-system"))
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with Matchers
    with ImplicitSender {
  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "custom assertion" in {
    final case class Update(id: Int, value: String)
    val probe = new TestProbe(system) {
      def expectUpdate(id: Int): Unit = {
        expectMsgPF() { case Update(`id`, _) =>
          ()
        }
        sender() ! "ACK"
      }
    }
    implicit val timeout: Timeout = 3.seconds

    val future = probe.ref ? Update(1, "some")
    probe.expectUpdate(1)
    future.isCompleted mustBe true
    future.value.get mustBe Success("ACK")
  }

  "watch other actors" in {
    val actor = system.actorOf(Props.empty)
    val probe = TestProbe()
    probe.watch(actor)
    actor ! PoisonPill
    probe.expectTerminated(actor)
  }

  "reply by probe" in {
    implicit val timeout: Timeout = 3.seconds

    val probe = TestProbe()
    val future = probe.ref ? "hello"
    probe.expectMsg("hello")
    probe.reply("world")
    future.isCompleted mustBe true
    future.value.get mustBe Success("world")
  }

  "message forwarding via probe" in {
    class Source(destination: ActorRef) extends Actor {
      override def receive = {
        case "start" => destination ! "work"
        case "done"  => context.stop(self)
      }
    }
    val probe = TestProbe()
    val probe2 = TestProbe()
    val source = system.actorOf(Props(new Source(probe.ref)))

    watch(source)
    source ! "start"
    probe.expectMsg("work")
    probe.forward(probe2.ref)
    probe2.expectMsg("work")
    probe2.reply("done")
    expectTerminated(source)
  }

  "auto pilot" in {
    val probe = TestProbe()
    probe.setAutoPilot(new testkit.TestActor.AutoPilot {
      override def run(sender: ActorRef, msg: Any): TestActor.AutoPilot = {
        msg match {
          case "stop" =>
            TestActor.NoAutoPilot
          case x =>
            testActor.tell(x, sender)
            TestActor.KeepRunning
        }
      }
    })
    probe.ref ! "message"
    expectMsg("message")
    probe.ref ! "stop"
    expectNoMessage()
    probe.ref ! "hi"
    probe.expectMsg("message")
    expectNoMessage()
  }

  "probe within" in {
    import system.dispatcher
    val probe = TestProbe()
    probe.within(1.seconds) {
      within(3.seconds) { // this is for testkit, and has no effect to own probe.
        akka.pattern
          .after(2.seconds)(Future.successful("message"))
          .pipeTo(probe.ref)
        probe.expectNoMessage()
      }
    }
  }

}
