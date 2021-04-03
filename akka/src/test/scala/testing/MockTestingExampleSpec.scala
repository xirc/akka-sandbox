package testing

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration.DurationInt
import scala.util.Success

final class MockTestingExampleSpec
    extends AnyWordSpec
    with BeforeAndAfterAll
    with Matchers {
  val testkit = ActorTestKit()
  override def afterAll(): Unit = {
    try super.afterAll()
    finally testkit.shutdownTestKit()
  }

  "test the happy path" in {
    val mockedBehavior = Behaviors.receiveMessage[Producer.Message] { message =>
      message.replyTo ! Success(message.i)
      Behaviors.same
    }
    val probe = testkit.createTestProbe[Producer.Message]()
    val mockedPublisher =
      testkit.spawn(Behaviors.monitor(probe.ref, mockedBehavior))

    import testkit.system
    implicit val timeout: Timeout = 3.seconds
    val producer = new Producer(mockedPublisher)

    producer.produce(messages = 3)
    for (i <- 0 until 3) {
      val message = probe.expectMessageType[Producer.Message]
      message.i shouldBe i
    }
  }

}
