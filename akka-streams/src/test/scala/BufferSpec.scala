import akka.Done
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent._
import scala.concurrent.duration._
import scala.math._

final class BufferSpec extends BaseSpec(ActorSystem("buffer-spec")) {

  "example of the async operator" in {
    val future = Source(1 to 3)
      .map { i =>
        println(s"A: $i"); i
      }
      .async
      .map { i =>
        println(s"B: $i"); i
      }
      .async
      .map { i =>
        println(s"C: $i"); i
      }
      .async
      .runWith(Sink.ignore)
    future.futureValue shouldBe Done
  }

  "example of internal buffers" in {
    val flow1 =
      Flow[Int].map(_ * 2).async.addAttributes(Attributes.inputBuffer(1, 1))
    println("using flow1")
    val future1 = Source(1 to 10).via(flow1).runForeach(println)
    future1.futureValue shouldBe Done

    println("using flow1 and flow2")
    val flow2 = flow1.via(Flow[Int].map(_ / 2)).async
    val runnableGraph2 =
      Source(1 to 10).via(flow2).toMat(Sink.foreach(println))(Keep.right)
    runnableGraph2.run().futureValue shouldBe Done

    val withOverriddenDefaults =
      runnableGraph2.withAttributes(Attributes.inputBuffer(64, 64))
    withOverriddenDefaults.run().futureValue shouldBe Done
  }

  "example of issues caused by internal buffers" in {
    case class Tick()

    val source = Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val zipper =
        builder.add(ZipWith[Tick, Int, Int]((tick, count) => count).async)
      val source0 = Source.tick(3.seconds, 3.seconds, Tick()).take(5)
      val source1 = Source
        .tick(1.second, 1.second, "message!")
        .conflateWithSeed(_ => 1)((count, _) => count + 1)
        .take(10)

      source0 ~> zipper.in0
      source1 ~> zipper.in1

      SourceShape(zipper.out)
    })
    val runnableGraph = source.toMat(Sink.foreach(println))(Keep.right)
    val future = runnableGraph.run()
    Await.ready(future, 20.seconds)

    val future2 =
      runnableGraph.withAttributes(Attributes.inputBuffer(1, 1)).run()
    Await.ready(future2, 20.seconds)
  }

  "example of the buffer operator" in {
    val source = Source(1 to 10)
    val sink =
      Flow[Int].throttle(1, 1.seconds).toMat(Sink.foreach(println))(Keep.right)

    println("buffer backpressure")
    val future1 = source.buffer(3, OverflowStrategy.backpressure).runWith(sink)
    Await.ready(future1, 15.seconds)

    println("buffer dropTail")
    val future2 = source.buffer(3, OverflowStrategy.dropTail).runWith(sink)
    Await.ready(future2, 15.seconds)

    println("buffer dropHead")
    val future4 = source.buffer(3, OverflowStrategy.dropHead).runWith(sink)
    Await.ready(future4, 15.seconds)

    println("buffer dropBuffer")
    val future5 = source.buffer(3, OverflowStrategy.dropBuffer).runWith(sink)
    Await.ready(future5, 15.seconds)

    println("buffer fail")
    val future6 = source.buffer(3, OverflowStrategy.fail).runWith(sink)
    Await.ready(future6, 15.seconds)
    future6.value.get.isFailure shouldBe true
  }

  "example of the conflate operator" in {
    val source = Source(1 to 10).map(_.toDouble).throttle(1, 0.3.seconds)
    val flow = Flow[Double].conflateWithSeed(Seq(_))(_ :+ _).map { seq =>
      val u = seq.sum / seq.size
      val se = seq.map(x => pow(x - u, 2))
      val sig = sqrt(se.sum / se.size)
      (sig, u, seq.size)
    }
    val sink = Flow[(Double, Double, Int)]
      .throttle(1, 1.seconds)
      .toMat(Sink.foreach(println))(Keep.right)

    val runnable = source.via(flow).toMat(sink)(Keep.right)
    val future = runnable.run()
    Await.ready(future, 10.seconds)
  }

  "example of the extrapolate operator" in {
    val source = Source(1 to 5).throttle(1, 1.seconds)
    val lastFlow =
      Flow[Int].extrapolate(Iterator.continually(_), Some(Int.MaxValue))
    val sink = Flow[Int]
      .throttle(1, 0.5.seconds)
      .toMat(Sink.foreach(println))(Keep.right)

    println("lastFlow")
    val future = source.via(lastFlow).runWith(sink)
    Await.ready(future, 10.seconds)

    println("driftFlow")
    val driftFlow =
      Flow[Int].map(_ -> 0).extrapolate[(Int, Int)] { case (i, _) =>
        Iterator.from(1).map(i -> _)
      }
    val sink2 = Flow[(Int, Int)]
      .throttle(1, 0.5.seconds)
      .toMat(Sink.foreach(println))(Keep.right)
    val future2 = source.via(driftFlow).runWith(sink2)
    Await.ready(future2, 10.seconds)
  }

  "example of the expand operator" in {
    val source = Source(1 to 5).throttle(1, 1.seconds)
    val driftFlow = Flow[Int].expand(i => Iterator.from(0).map(i -> _))
    val sink = Flow[(Int, Int)]
      .throttle(1, 0.5.seconds)
      .toMat(Sink.foreach(println))(Keep.right)
    val future = source.via(driftFlow).runWith(sink)
    Await.ready(future, 10.seconds)
  }

}
