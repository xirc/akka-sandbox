package testing

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration.DurationInt

final class AsyncTestingExampleSpec
  extends AnyWordSpec
  with BeforeAndAfterAll
  with Matchers
{
  val testkit = ActorTestKit()
  override def afterAll(): Unit = {
    try super.afterAll()
    finally testkit.shutdownTestKit()
  }

  "basic example" in {
    val pinger = testkit.spawn(Echo(), "ping")
    val probe = testkit.createTestProbe[Echo.Pong]()
    pinger ! Echo.Ping("hello", probe.ref)
    probe.expectMessage(Echo.Pong("hello"))
  }

  "stop example" in {
    val probe = testkit.createTestProbe[Echo.Pong]()
    val pinger1 = testkit.spawn(Echo(), "pinger")

    pinger1 ! Echo.Ping("hello", probe.ref)
    probe.expectMessage(Echo.Pong("hello"))
    testkit.stop(pinger1)

    val pinger2 = testkit.spawn(Echo(), "pinger")
    pinger2 ! Echo.Ping("hello", probe.ref)
    probe.expectMessage(Echo.Pong("hello"))
    testkit.stop(pinger2, 10.seconds)
  }
}
