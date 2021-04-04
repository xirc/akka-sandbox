import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.jdk.DurationConverters.JavaDurationOps

abstract class BaseSpec(system: ActorSystem)
    extends TestKit(system)
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with DefaultTimeout {

  private lazy val _patienceConfig = {
    PatienceConfig(
      scaled(
        system.settings.config.getDuration("akka.test.default-timeout").toScala
      )
    )
  }

  private lazy val _spanScaleFactor = {
    testKitSettings.TestTimeFactor
  }

  override implicit def patienceConfig: PatienceConfig = _patienceConfig

  override def spanScaleFactor: Double = _spanScaleFactor

  override def afterAll(): Unit = {
    try TestKit.shutdownActorSystem(system)
    finally super.afterAll()
  }

}
