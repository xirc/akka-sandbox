import akka.actor.{Actor, ActorSystem, Props}
import akka.stream._
import akka.stream.scaladsl._
import akka.{Done, NotUsed}

import scala.concurrent._
import scala.util.{Failure, Success}

final class BasicSpec extends BaseSpec(ActorSystem("test")) {

  "example of toMat" in {
    val source = Source(1 to 10)
    val sink = Sink.fold[Int, Int](0)(_ + _)
    val runnable = source.toMat(sink)(Keep.right)
    runnable.run().futureValue shouldBe 55
  }

  "example of runWith" in {
    val source = Source(1 to 10)
    val sink = Sink.fold[Int, Int](0)(_ + _)
    source.runWith(sink).futureValue shouldBe 55
  }

  "example of immutability of operators" in {
    val source = Source(1 to 10)

    source.map(_ => 0) // has no effect on source, since it's immutable
    source.runWith(Sink.fold(0)(_ + _)).futureValue shouldBe 55

    val zeros = source.map(_ => 0)
    zeros.runWith(Sink.fold(0)(_ + _)).futureValue shouldBe 0
  }

  "example of multiple times materialization" in {
    val sink = Sink.fold[Int, Int](0)(_ + _)
    val runnable = Source(1 to 10).toMat(sink)(Keep.right)
    val sumFuture1 = runnable.run()
    val sumFuture2 = runnable.run()

    sumFuture1 should not be sumFuture2
    sumFuture1.futureValue shouldBe sumFuture2.futureValue
  }

  "example of various source constructors" in {
    val listSource: Source[Int, NotUsed] =
      Source(List(1, 2, 3))
    listSource.runReduce(_ + _).futureValue shouldBe 6

    val futureSource: Source[String, NotUsed] =
      Source.future(Future.successful("Hello Streams!"))
    futureSource.runReduce(_ + _).futureValue shouldBe "Hello Streams!"

    val singleSource: Source[String, NotUsed] =
      Source.single("only one element")
    singleSource.runReduce(_ + _).futureValue shouldBe "only one element"

    val emptySource: Source[Int, NotUsed] =
      Source.empty[Int]
    emptySource.runFold(0)(_ + _).futureValue shouldBe 0

  }

  "example of various sink constructors" in {
    val foldSink: Sink[Int, Future[Int]] =
      Sink.fold[Int, Int](0)(_ + _)
    Source(Seq(1, 2, 3)).runWith(foldSink).futureValue shouldBe 6

    val headSink: Sink[Int, Future[Int]] =
      Sink.head[Int]
    Source(Seq(1, 2, 3)).runWith(headSink).futureValue shouldBe 1

    val ignoreSink: Sink[Any, Future[Done]] =
      Sink.ignore
    Source(Seq("1", "2", "3")).runWith(ignoreSink).futureValue shouldBe Done

    val foreachSink: Sink[String, Future[Done]] =
      Sink.foreach[String](println)
    Source(Seq("1", "2", "3")).runWith(foreachSink).futureValue shouldBe Done

  }

  "example of various ways to wire up parts of streams" in {
    // Explicitly creating and wiring up a Source, Sink, and Flow
    Source(1 to 6)
      .via(Flow[Int].map(_ * 2))
      .to(Sink.foreach(println))

    // Starting from a Source
    val source = Source(1 to 6).map(_ * 2)
    source.to(Sink.foreach(println))

    // Starting from a Sink
    val sink = Flow[Int].map(_ * 2).to(Sink.foreach(println))
    Source(1 to 6).to(sink)

    // Broadcast to a sink inline
    val otherSink = Flow[Int].alsoTo(Sink.foreach(println)).to(Sink.ignore)
    Source(1 to 6).to(otherSink)
  }

  "example of illegal stream elements" in {
    val illegalSource = Source(List("abc", null, "def"))
    val failureFuture =
      illegalSource.toMat(Sink.fold("")(_ + _))(Keep.right).run()
    failureFuture.failed.futureValue shouldBe a[NullPointerException]

    val legalSource = Source(List("abc", null, "def").map(Option(_)))
    val future =
      legalSource.toMat(Sink.fold("")(_ + _.getOrElse("")))(Keep.right).run()
    future.futureValue shouldBe "abcdef"
  }

  "example of source pre-materialization" in {
    val matValuePoweredSource =
      Source.queue[String](
        bufferSize = 100,
        overflowStrategy = OverflowStrategy.fail
      )
    val (queue, source) = matValuePoweredSource.preMaterialize()

    val resultFuture = source.runWith(Sink.head[String])

    queue.offer("Hello!")
    queue.offer("World!")
    queue.complete()
    resultFuture.futureValue shouldBe "Hello!"
  }

  "example of actor materializer lifecycle" in {
    final class RunWithMyself(promise: Promise[Done]) extends Actor {
      implicit val mat: Materializer = Materializer(context)

      Source.maybe.runWith(Sink.onComplete({
        case Success(done) =>
          println(s"Completed: $done")
          promise.success(done)
        case Failure(ex) =>
          println(s"Failed: ${ex.getMessage}")
          promise.failure(ex)
      }))

      override def receive: Receive = { case "boom" =>
        context.stop(self)
      }
    }

    val promise = Promise[Done]()
    val actor = system.actorOf(Props(new RunWithMyself(promise)))
    watch(actor)
    actor ! "boom"
    expectTerminated(actor)
    promise.future.failed.futureValue shouldBe a[
      AbruptStageTerminationException
    ]
  }

  "example of explicit materializer lifecycle" in {
    final class RunWithMyself(promise: Promise[Done])(implicit
        val materializer: Materializer
    ) extends Actor {
      Source.maybe.runWith(Sink.onComplete({
        case Success(done) =>
          println(s"Completed: $done")
          promise.success(done)
        case Failure(ex) =>
          println(s"Failed: ${ex.getMessage}")
          promise.failure(ex)
      }))
      override def receive: Receive = { case "boom" =>
        context.stop(self)
      }
    }

    implicit val materializer: Materializer = Materializer(system)
    val promise = Promise[Done]()
    val actor = system.actorOf(Props(new RunWithMyself(promise)))

    watch(actor)
    actor ! "boom"
    expectTerminated(actor)

    promise.isCompleted shouldBe false
    materializer.shutdown()
    awaitCond(materializer.isShutdown)
    promise.future.failed.futureValue shouldBe a[
      AbruptStageTerminationException
    ]
  }

}
