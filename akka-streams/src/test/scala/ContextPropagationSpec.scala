import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent._
import scala.concurrent.duration._

final class ContextPropagationSpec
    extends TestKit(ActorSystem("context-propagation-spec"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "example of context propagation" in {
    case class Context(meta: String)
    val source = Source(List(1, 2, 3)).asSourceWithContext(value =>
      Context(value.toString)
    )
    val flow = FlowWithContext[Int, Context].map(_ * 2)
    val sink = Sink.seq[(Int, Context)]
    val future = source.via(flow).runWith(sink)
    val result = Await.result(future, 3.seconds)
    result shouldBe {
      Seq(
        (2, Context("1")),
        (4, Context("2")),
        (6, Context("3"))
      )
    }
  }

}
