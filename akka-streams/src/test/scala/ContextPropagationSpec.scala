import akka.actor.ActorSystem
import akka.stream.scaladsl._

final class ContextPropagationSpec
    extends BaseSpec(ActorSystem("context-propagation-spec")) {

  "example of context propagation" in {
    case class Context(meta: String)
    val source = Source(List(1, 2, 3)).asSourceWithContext(value =>
      Context(value.toString)
    )
    val flow = FlowWithContext[Int, Context].map(_ * 2)
    val sink = Sink.seq[(Int, Context)]
    val result = source.via(flow).runWith(sink).futureValue
    result shouldBe {
      Seq(
        (2, Context("1")),
        (4, Context("2")),
        (6, Context("3"))
      )
    }
  }

}
