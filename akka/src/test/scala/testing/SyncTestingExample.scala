package testing

import akka.actor.testkit.typed.CapturedLogEvent
import akka.actor.testkit.typed.Effect.{Spawned, SpawnedAnonymous}
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.event.Level

final class SyncTestingExample
  extends AnyWordSpec
  with Matchers
{
  "Hello" should {
    "spawn children with a name" in {
      val testkit = BehaviorTestKit(Hello())
      testkit.run(Hello.CreateChild("child"))
      testkit.expectEffect(Spawned(Hello.childActor, "child"))
    }
    "spawn anonymous children" in {
      val testkit = BehaviorTestKit(Hello())
      testkit.run(Hello.CreateAnonymousChild)
      testkit.expectEffect(SpawnedAnonymous(Hello.childActor))
    }
    "send messages" in {
      val testkit = BehaviorTestKit(Hello())
      val inbox = TestInbox[String]()
      testkit.run(Hello.SayHello(inbox.ref))
      inbox.expectMessage("hello")
    }
    "send messages to children with a name" in {
      val testkit = BehaviorTestKit(Hello())
      testkit.run(Hello.SayHelloToChild("child"))
      val childInbox = testkit.childInbox[String]("child")
      childInbox.expectMessage("hello")
    }
    "send messages to anonymous children" in {
      val testkit = BehaviorTestKit(Hello())
      testkit.run(Hello.SayHelloToAnonymousChild)
      val child = testkit.expectEffectType[SpawnedAnonymous[String]]

      val childInbox = testkit.childInbox(child.ref)
      childInbox.expectMessage("hello stranger")
    }
    "log messages" in {
      val testkit = BehaviorTestKit(Hello())
      val inbox = TestInbox[String]("Inboxer")
      testkit.run(Hello.LogAndSayHello(inbox.ref))
      testkit.logEntries() shouldBe Seq(
        CapturedLogEvent(Level.INFO, "Saying hello to Inboxer")
      )
    }
  }
}
