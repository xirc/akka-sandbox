import akka.actor.{ActorKilledException, ActorSystem, Kill, Props}
import akka.testkit.{EventFilter, TestKit}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

object MyEventFilterSpec {
  def config: Config =
    ConfigFactory.parseString("""
        |akka.loggers = ["akka.testkit.TestEventListener"]
        |""".stripMargin)
}
class MyEventFilterSpec
    extends TestKit(ActorSystem("test-system", MyEventFilterSpec.config))
    with AnyWordSpecLike
    with BeforeAndAfterAll {
  "expect actor kill exception" in {
    val actor = system.actorOf(Props.empty)
    watch(actor)
    EventFilter[ActorKilledException](occurrences = 1) intercept {
      actor ! Kill
    }
    expectTerminated(actor)
  }

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
