import java.nio.ByteOrder

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage.{GraphStageLogic, InHandler, OutHandler}
import akka.testkit.{TestActors, TestKit}
import akka.util.ByteString
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.collection.immutable
import scala.concurrent._
import scala.concurrent.duration._

final class GraphSpec
    extends TestKit(ActorSystem("graph-spec"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "example of a simple GraphDSL" in {
    val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val in = Source(1 to 10)
      val out = Sink.ignore

      val bcast = builder.add(Broadcast[Int](2))
      val merge = builder.add(Merge[Int](2))

      val f1, f2, f3, f4 = Flow[Int].map(_ + 10)
      in ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> out
      bcast ~> f4 ~> merge

      ClosedShape
    })
    g.run()
  }

  "example of two parallel stream in a graph" in {
    val topHeadSink = Sink.head[Int]
    val bottomHeadSink = Sink.head[Int]
    val sharedDoubler = Flow[Int].map(_ * 2)

    val g = RunnableGraph.fromGraph(
      GraphDSL.create(topHeadSink, bottomHeadSink)((_, _)) {
        implicit builder => (topHs, bottomHs) =>
          import GraphDSL.Implicits._

          val broadcast = builder.add(Broadcast[Int](2))
          Source.single(1) ~> broadcast.in
          broadcast ~> sharedDoubler ~> topHs.in
          broadcast ~> sharedDoubler ~> bottomHs.in

          ClosedShape
      }
    )

    val (trf, brf) = g.run()
    val topResult = Await.result(trf, 3.seconds)
    val bottomResult = Await.result(brf, 3.seconds)
    topResult shouldBe 2
    bottomResult shouldBe 2
  }

  "example of many stream in a graph" in {
    val sinks = immutable
      .Seq("a", "b", "c")
      .map(prefix => {
        Flow[String]
          .filter(str => str.startsWith(prefix))
          .toMat(Sink.head[String])(Keep.right)
      })

    val g = RunnableGraph.fromGraph(GraphDSL.create(sinks) {
      implicit builder => sinkList =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[String](sinkList.size))
        Source(List("xa", "xb", "xc", "ax", "bx", "cx")) ~> broadcast
        sinkList.foreach(sink => broadcast ~> sink)

        ClosedShape
    })

    import system.dispatcher
    val futureList: Seq[Future[String]] = g.run()
    val listFuture: Future[Seq[String]] = Future.traverse(futureList)(identity)
    val list = Await.result(listFuture, 3.seconds)
    list should be(Seq("ax", "bx", "cx"))
  }

  "example of a partial graph (flow)" in {
    val pickMaxOfThee = GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // In application, you should use one ZipWith[Int,Int,Int,Int].
      val zip1 = builder.add(ZipWith[Int, Int, Int](math.max))
      val zip2 = builder.add(ZipWith[Int, Int, Int](math.max))
      zip1.out ~> zip2.in0

      UniformFanInShape(zip2.out, zip1.in0, zip1.in1, zip2.in1)
    }

    val resultSink = Sink.head[Int]

    val g = RunnableGraph.fromGraph(GraphDSL.create(resultSink) {
      implicit builder => sink =>
        import GraphDSL.Implicits._

        val pm3 = builder.add(pickMaxOfThee)

        Source.single(1) ~> pm3.in(0)
        Source.single(2) ~> pm3.in(1)
        Source.single(3) ~> pm3.in(2)

        pm3.out ~> sink.in

        ClosedShape
    })

    val maxFuture: Future[Int] = g.run()
    val result = Await.result(maxFuture, 3.seconds)
    result should be(3)
  }

  "example of constructing a source from a partial graph" in {
    val pairs = Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val zip = builder.add(Zip[Int, Int]())
      val ints = Source.fromIterator(() => Iterator.from(1))

      ints.filter(_ % 2 != 0) ~> zip.in0
      ints.filter(_ % 2 == 0) ~> zip.in1

      SourceShape(zip.out)
    })

    val firstPairFuture: Future[(Int, Int)] = pairs.runWith(Sink.head)
    val firstPair = Await.result(firstPairFuture, 3.seconds)
    firstPair should be((1, 2))
  }

  "example of constructing a flow from a partial graph" in {
    val pairUpWithString =
      Flow.fromGraph(GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[Int](2))
        val zip = builder.add(Zip[Int, String]())

        broadcast.out(0).map(identity) ~> zip.in0
        broadcast.out(1).map(_.toString) ~> zip.in1

        FlowShape(broadcast.in, zip.out)
      })

    val (_, headFuture) =
      pairUpWithString.runWith(Source(List(1, 2)), Sink.head)
    val head = Await.result(headFuture, 3.seconds)
    head should be((1, "1"))
  }

  "example of combining sources" in {
    val source1 = Source(List(1))
    val source2 = Source(List(2))
    val merged = Source.combine(source1, source2)(Merge(_))

    val resultFuture = merged.runWith(Sink.fold(0)(_ + _))
    val result = Await.result(resultFuture, 3.seconds)
    result should be(3)
  }

  "example of combining sinks" in {
    val actorRef = testActor
    val sendRemotely = Sink.actorRef(actorRef, "Done")
    val localProcessing = Sink.foreach(println)
    val sink = Sink.combine(sendRemotely, localProcessing)(Broadcast[Int](_))

    Source(List(0, 1, 2)).runWith(sink)
    expectMsg(0)
    expectMsg(1)
    expectMsg(2)
  }

  "example of building reusable graph components" in {
    case class PriorityWorkerPoolShape[In, Out](
        jobsIn: Inlet[In],
        priorityJobsIn: Inlet[In],
        resultsOut: Outlet[Out]
    ) extends Shape {
      override def inlets: immutable.Seq[Inlet[_]] =
        jobsIn :: priorityJobsIn :: Nil
      override def outlets: immutable.Seq[Outlet[_]] = resultsOut :: Nil
      override def deepCopy(): Shape =
        PriorityWorkerPoolShape(
          jobsIn.carbonCopy(),
          priorityJobsIn.carbonCopy(),
          resultsOut.carbonCopy()
        )
    }

    import FanInShape.{Init, Name}
    class PriorityWorkerPoolShape2[In, Out](
        _init: Init[Out] = Name("PriorityWorkerPool")
    ) extends FanInShape[Out](_init) {
      override protected def construct(init: Init[Out]): FanInShape[Out] =
        new PriorityWorkerPoolShape2(init)
      val jobsIn = newInlet[In]("jobsIn")
      val priorityJobsIn = newInlet[In]("priorityJobsIn")
    }

    object PriorityWorkerPool {
      def apply[In, Out](
          worker: Flow[In, Out, Any],
          workerCount: Int
      ): Graph[PriorityWorkerPoolShape[In, Out], NotUsed] = {
        GraphDSL.create() { implicit builder =>
          import GraphDSL.Implicits._

          val priorityMerge = builder.add(MergePreferred[In](1))
          val balance = builder.add(Balance[In](workerCount))
          val resultsMerge = builder.add(Merge[Out](workerCount))

          priorityMerge ~> balance

          for (i <- 0 until workerCount) {
            balance.out(i) ~> worker ~> resultsMerge.in(i)
          }

          PriorityWorkerPoolShape(
            priorityMerge.in(0),
            priorityMerge.preferred,
            resultsMerge.out
          )
        }
      }
    }

    val worker1 = Flow[String].map("step 1 " + _)
    val worker2 = Flow[String].map("step 2 " + _)

    val runnableGraph =
      RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val priorityPool1 = builder.add(PriorityWorkerPool(worker1, 4))
        val priorityPool2 = builder.add(PriorityWorkerPool(worker2, 2))

        Source(1 to 100).map("job: " + _) ~> priorityPool1.jobsIn
        Source(1 to 100).map(
          "priority job: " + _
        ) ~> priorityPool1.priorityJobsIn

        priorityPool1.resultsOut ~> priorityPool2.jobsIn
        Source(1 to 100).map(
          "one-step, priority " + _
        ) ~> priorityPool2.priorityJobsIn

        priorityPool2.resultsOut ~> Sink.foreach(println)
        ClosedShape
      })
    runnableGraph.run()
  }

  "example of bidirectional flow" in {
    sealed trait Message
    case class Ping(id: Int) extends Message
    case class Pong(id: Int) extends Message

    def toBytes(msg: Message): ByteString = {
      implicit val order = ByteOrder.LITTLE_ENDIAN
      msg match {
        case Ping(id) =>
          ByteString.newBuilder.putByte(1).putInt(id).result()
        case Pong(id) =>
          ByteString.newBuilder.putByte(2).putInt(id).result()
      }
    }

    def fromBytes(bytes: ByteString): Message = {
      implicit val order = ByteOrder.LITTLE_ENDIAN
      val it = bytes.iterator
      it.getByte match {
        case 1 => Ping(it.getInt)
        case 2 => Pong(it.getInt)
        case other =>
          throw new RuntimeException(s"parse error: expected 1|2 got $other")
      }
    }

    val codecVerbose =
      BidiFlow.fromGraph(GraphDSL.create() { implicit builder =>
        val outbound = builder.add(Flow[Message].map(toBytes))
        val inbound = builder.add(Flow[ByteString].map(fromBytes))
        BidiShape.fromFlows(outbound, inbound)
      })
    // or this is same as the above
    val codec = BidiFlow.fromFunctions(toBytes, fromBytes)

    val framing = BidiFlow.fromGraph(GraphDSL.create() { implicit builder =>
      implicit val order = ByteOrder.LITTLE_ENDIAN
      import akka.stream.stage.GraphStage

      def addLengthHeader(bytes: ByteString) = {
        val len = bytes.length
        ByteString.newBuilder.putInt(len).append(bytes).result()
      }

      class FrameParser extends GraphStage[FlowShape[ByteString, ByteString]] {
        val in = Inlet[ByteString]("FrameParser.in")
        val out = Outlet[ByteString]("FrameParser.out")

        override val shape: FlowShape[ByteString, ByteString] =
          FlowShape.of(in, out)
        override def createLogic(
            inheritedAttributes: Attributes
        ): GraphStageLogic = new GraphStageLogic(shape) {
          var stash = ByteString.empty
          var needed = -1

          setHandler(
            out,
            new OutHandler {
              override def onPull(): Unit = {
                if (isClosed(in)) run()
                else pull(in)
              }
            }
          )
          setHandler(
            in,
            new InHandler {
              override def onPush(): Unit = {
                val bytes = grab(in)
                stash ++= bytes
                run()
              }

              override def onUpstreamFinish(): Unit = {
                if (stash.isEmpty) completeStage()
                else if (isAvailable(out)) run()
              }
            }
          )

          private def run(): Unit = {
            if (needed == -1) {
              if (stash.length < 4) {
                if (isClosed(in)) completeStage()
                else pull(in)
              } else {
                needed = stash.iterator.getInt
                stash = stash.drop(4)
                run()
              }
            } else if (stash.length < needed) {
              if (isClosed(in)) completeStage()
              else pull(in)
            } else {
              val emit = stash.take(needed)
              stash = stash.drop(needed)
              needed = -1
              push(out, emit)
            }
          }
        }
      }

      val outbound = builder.add(Flow[ByteString].map(addLengthHeader))
      val inbound = builder.add(Flow[ByteString].via(new FrameParser))
      BidiShape.fromFlows(outbound, inbound)
    })

    val stack = codec.atop(framing)

    val pingpong = Flow[Message].collect { case Ping(id) => Pong(id) }
    val flow = stack.atop(stack.reversed).join(pingpong)
    val resultFuture =
      Source((0 to 9).map(Ping)).via(flow).limit(20).runWith(Sink.seq)
    val result = Await.result(resultFuture, 3.seconds)
    result should be((0 to 9).map(Pong))
  }

  "example of accessing the materialized value inside the graph" in {
    val foldFlow: Flow[Int, Int, Future[Int]] =
      Flow.fromGraph(GraphDSL.create(Sink.fold[Int, Int](0)(_ + _)) {
        implicit builder => fold =>
          import GraphDSL.Implicits._
          val flow = builder.materializedValue.mapAsync(4)(identity)
          FlowShape(fold.in, flow.outlet)
      })
    val resultFuture = Source(List(1, 2, 3)).via(foldFlow).runWith(Sink.seq)
    val result = Await.result(resultFuture, 3.seconds)
    result should be(Seq(6))
  }

}
