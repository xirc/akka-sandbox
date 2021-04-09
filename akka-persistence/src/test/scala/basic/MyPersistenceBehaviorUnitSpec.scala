package basic

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit.SerializationSettings
import akka.persistence.typed.PersistenceId
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike

final class MyPersistenceBehaviorUnitSpec
    extends ScalaTestWithActorTestKit(EventSourcedBehaviorTestKit.config)
    with AnyWordSpecLike
    with BeforeAndAfterEach {

  private val esTestKit =
    EventSourcedBehaviorTestKit[
      MyPersistenceBehavior.Command,
      MyPersistenceBehavior.Event,
      MyPersistenceBehavior.State
    ](
      system,
      MyPersistenceBehavior(PersistenceId.ofUniqueId("history")),
      SerializationSettings.enabled.withVerifyEquality(true)
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    esTestKit.clear()
  }

  "MyPersistenceBehavior" should {

    "add items" in {
      val result1 = esTestKit.runCommand(MyPersistenceBehavior.Add("hello"))
      result1.event shouldBe MyPersistenceBehavior.Added("hello")
      result1.state.history.items.head shouldBe "hello"

      val result2 = esTestKit.runCommand(MyPersistenceBehavior.Add("world"))
      result2.event shouldBe MyPersistenceBehavior.Added("world")
      result2.state.history.items.head shouldBe "world"
    }

    "clear history" in {
      esTestKit.runCommand(MyPersistenceBehavior.Add("hello"))
      esTestKit.runCommand(MyPersistenceBehavior.Add("world"))

      val result = esTestKit.runCommand(MyPersistenceBehavior.Clear)
      result.event shouldBe MyPersistenceBehavior.Cleared
      result.state.history shouldBe empty
    }

    "get history" in {
      val initialResult = esTestKit.runCommand(MyPersistenceBehavior.GetHistory)
      initialResult.hasNoEvents shouldBe true
      initialResult.state.history shouldBe empty

      esTestKit.runCommand(MyPersistenceBehavior.Add("hello"))
      esTestKit.runCommand(MyPersistenceBehavior.Add("world"))

      val hasTwoItems = esTestKit.runCommand(MyPersistenceBehavior.GetHistory)
      hasTwoItems.hasNoEvents shouldBe true
      hasTwoItems.state.history.items shouldBe List("world", "hello")
    }

    "recover its state from persisted data" in {
      esTestKit.runCommand(MyPersistenceBehavior.Add("hello"))
      esTestKit.runCommand(MyPersistenceBehavior.Add("world"))

      val beforeRecovered =
        esTestKit.runCommand(MyPersistenceBehavior.GetHistory)
      beforeRecovered.state.history.items shouldBe List("world", "hello")

      esTestKit.restart()

      // Recovery should be invoked
      val afterRecovered =
        esTestKit.runCommand(MyPersistenceBehavior.GetHistory)
      afterRecovered.state.history.items shouldBe List("world", "hello")
    }

  }

}
