import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.Tcp.OutgoingConnection
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.annotation.nowarn
import scala.concurrent._

final class StreamCompositionSpec
    extends BaseSpec(ActorSystem("stream-composition-spec")) {

  "example of nested composition" in {
    val trivialRunnableGraph: RunnableGraph[Future[Int]] =
      Source
        .single(0)
        .map(_ + 1)
        .filter(_ != 0)
        .map(_ - 2)
        .toMat(Sink.fold(0)(_ + _))(Keep.right)

    val nestedSource: Source[Int, NotUsed] =
      Source
        .single(0)
        .map(_ + 1)
        .named("nestedSource")

    val nestedFlow: Flow[Int, Int, NotUsed] =
      Flow[Int]
        .filter(_ != 0)
        .map(_ - 2)
        .named("nestedFlow")

    val nestedSink: Sink[Int, Future[Int]] =
      nestedFlow
        .toMat(Sink.fold(0)(_ + _))(Keep.right)
        .named("nestedSink")

    val runnableGraph: RunnableGraph[Future[Int]] =
      nestedSource.toMat(nestedSink)(Keep.right)

    val trivialResultFuture = trivialRunnableGraph.run()
    val resultFuture = runnableGraph.run()
    trivialResultFuture.futureValue shouldBe -1
    resultFuture.futureValue shouldBe -1
  }

  "example of a complex runnable graph" in {
    val runnableGraph =
      RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val A = builder.add(Source.single(0)).out
        val B = builder.add(Broadcast[Int](2))
        val C = builder.add(Merge[Int](2))
        val D = builder.add(Flow[Int].map(_ + 1))
        val E = builder.add(Balance[Int](2))
        val F = builder.add(Merge[Int](2))
        val G = builder.add(Sink.foreach(println)).in

        C <~ F
        A ~> B ~> C ~> F
        B ~> D ~> E ~> F
        E ~> G

        ClosedShape
      })

    runnableGraph.run()
  }

  "example of a reusable runnable graph" in {
    val partial = GraphDSL
      .create() { implicit builder =>
        import GraphDSL.Implicits._

        val B = builder.add(Broadcast[Int](2))
        val C = builder.add(Merge[Int](2))
        val E = builder.add(Balance[Int](2))
        val F = builder.add(Merge[Int](2))

        C <~ F
        B ~> C ~> F
        B ~> Flow[Int].map(_ + 1) ~> E ~> F

        FlowShape(B.in, E.out(1))
      }
      .named("partial")

    val runnableGraph = Source.single(1).via(partial).to(Sink.foreach(println))
    runnableGraph.run()

    // treat `partial` as flow
    val flow = Flow.fromGraph(partial)

    val source = Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val merge = builder.add(Merge[Int](2))
      Source.single(0) ~> merge
      Source(List(2, 3, 4)) ~> merge

      SourceShape(merge.out)
    })

    val sink = {
      val nestedFlow = Flow[Int].map(_ * 2).drop(10).named("nestedFlow")
      nestedFlow.toMat(Sink.head)(Keep.right)
    }

    val runnableGraph2 = source.via(flow.filter(_ > 1)).toMat(sink)(Keep.right)
    runnableGraph2.run()
  }

  "example of composition of a closed graph in another one" in {
    val closed1 = Source.single(0).to(Sink.foreach(println))
    @nowarn
    val closed2 =
      RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val embeddedClosedShape = builder.add(closed1)

        // .. do something

        embeddedClosedShape
      })
    closed2.run()
  }

  "example of materialized value" in {
    val source: Source[Int, Promise[Option[Int]]] = Source.maybe[Int]
    val flow1: Flow[Int, Int, NotUsed] = Flow[Int].take(100)
    val nestedSource: Source[Int, Promise[Option[Int]]] =
      source.viaMat(flow1)(Keep.left).named("nestedSource")

    val flow2: Flow[Int, ByteString, NotUsed] = Flow[Int].map { i =>
      ByteString(i.toString)
    }
    val flow3: Flow[ByteString, ByteString, Future[OutgoingConnection]] =
      Tcp().outgoingConnection("localhost", 8080)
    val nestedFlow: Flow[Int, ByteString, Future[OutgoingConnection]] =
      flow2.viaMat(flow3)(Keep.right).named("nestedFlow")

    val sink: Sink[ByteString, Future[String]] = Sink.fold("")(_ + _.utf8String)
    val nestedSink: Sink[Int, (Future[OutgoingConnection], Future[String])] =
      nestedFlow.toMat(sink)(Keep.both)

    case class MyClass(
        private val p: Promise[Option[Int]],
        connection: OutgoingConnection
    ) {
      def close() = p.trySuccess(None)
    }

    def f(
        p: Promise[Option[Int]],
        rest: (Future[OutgoingConnection], Future[String])
    )(implicit executionContext: ExecutionContext): Future[MyClass] = {
      val connectionFuture = rest._1
      connectionFuture.map(MyClass(p, _))
    }

    import system.dispatcher
    val runnableGraph: RunnableGraph[Future[MyClass]] =
      nestedSource.toMat(nestedSink)(f)
    runnableGraph.run()
  }
}
