import akka.actor.{Actor, ActorSystem}
import akka.pattern.ask
import akka.testkit.{DefaultTimeout, TestActorRef, TestKit}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class MyTestActorRefSpec
    extends TestKit(ActorSystem("test-system"))
    with AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with DefaultTimeout {
  class MyActor extends Actor {
    private var nrOfPing: Int = 0
    def getNrOfPing: Int = nrOfPing
    override def receive: Receive = { case "ping" =>
      nrOfPing += 1
      sender() ! "pong"
    }
  }

  "use TestActorRef" in {
    implicit def self = testActor
    val actorRef = TestActorRef(new MyActor)
    actorRef.underlyingActor.getNrOfPing mustBe 0
    actorRef ! "ping"
    expectMsg("pong")
    actorRef.underlyingActor.getNrOfPing mustBe 1
  }

  "ask to TestActorRef" in {
    val actorRef = TestActorRef(new MyActor)
    val future = actorRef ? "ping"
    future.futureValue must be("pong")
  }

  "test expecting exceptions" in {
    val actor = TestActorRef(new Actor {
      override def receive: Receive = { case "hello" =>
        throw new IllegalArgumentException("boom")
      }
    })
    intercept[IllegalArgumentException] {
      actor.receive("hello")
    }
  }
}
