package basic

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.persistence.testkit.scaladsl.{PersistenceTestKit, SnapshotTestKit}
import akka.persistence.testkit.{
  PersistenceTestKitPlugin,
  PersistenceTestKitSnapshotPlugin
}
import akka.persistence.typed.PersistenceId
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.{Await, Future}

final class MyPersistenceBehaviorSpec
    extends ScalaTestWithActorTestKit(MyPersistenceBehaviorSpec.config)
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterEach {

  private val persistenceTestKit = PersistenceTestKit(system)
  private val snapshotTestKit = SnapshotTestKit(system)

  override def beforeEach(): Unit = {
    super.beforeEach()
    persistenceTestKit.clearAll()
  }

  MyPersistenceBehavior.getClass.getSimpleName should {
    "persist all events" in {
      val persistenceId = PersistenceId.ofUniqueId("history")
      val bucketActor = spawn(MyPersistenceBehavior(persistenceId))

      bucketActor ! MyPersistenceBehavior.Add("hello")
      persistenceTestKit.expectNextPersisted(
        persistenceId.id,
        MyPersistenceBehavior.Added("hello")
      )

      await(bucketActor.ask(MyPersistenceBehavior.GetHistory))
      persistenceTestKit.expectNothingPersisted(persistenceId.id)

      bucketActor ! MyPersistenceBehavior.Clear
      persistenceTestKit.expectNextPersisted(
        persistenceId.id,
        MyPersistenceBehavior.Cleared
      )
    }

    "take snapshot every 3 events" in {
      val persistenceId = PersistenceId.ofUniqueId("history")
      val bucketActor = spawn(MyPersistenceBehavior(persistenceId))

      bucketActor ! MyPersistenceBehavior.Add("hello")
      snapshotTestKit.expectNothingPersisted(persistenceId.id)
      bucketActor ! MyPersistenceBehavior.Add("new")
      snapshotTestKit.expectNothingPersisted(persistenceId.id)
      bucketActor ! MyPersistenceBehavior.Add("world")
      snapshotTestKit.expectNextPersisted(
        persistenceId.id,
        MyPersistenceBehavior.State(
          MyPersistenceBehavior.History("world", "new", "hello")
        )
      )
    }

  }

  private def await[T](future: Future[T]): T =
    Await.result(future, timeout.duration)
}

object MyPersistenceBehaviorSpec {
  private val config =
    PersistenceTestKitPlugin.config
      .withFallback(PersistenceTestKitSnapshotPlugin.config)
      .withFallback(ConfigFactory.load)
}
