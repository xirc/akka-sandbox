import akka.actor.{ActorSystem, Status}
import akka.pattern.pipe
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.{Done, pattern}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent._
import scala.concurrent.duration._

final class TestkitSpec
    extends TestKit(ActorSystem("testkit"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with ImplicitSender {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "example of a test for a sink" in {
    val sinkUnderTest =
      Flow[Int].map(_ * 2).toMat(Sink.fold(0)(_ + _))(Keep.right)

    val future = Source(1 to 4).runWith(sinkUnderTest)
    val result = Await.result(future, 3.seconds)
    result should be(20)
  }

  "example of a test for a source" in {
    val sourceUnderTest = Source.repeat(1).map(_ * 2)

    val future = sourceUnderTest.take(10).runWith(Sink.seq)
    val result = Await.result(future, 3.seconds)
    result shouldBe Seq.fill(10)(2)
  }

  "example of a test for a flow" in {
    val flowUnderTest = Flow[Int].takeWhile(_ < 5)

    val future = Source(1 to 10)
      .via(flowUnderTest)
      .runWith(Sink.fold(Seq.empty[Int])(_ :+ _))
    val result = Await.result(future, 3.seconds)
    result shouldBe (1 to 4)
  }

  "example of using pipeTo" in {
    import system.dispatcher
    val sourceUnderTest = Source(1 to 4).grouped(2)
    val probe = TestProbe()
    sourceUnderTest.runWith(Sink.seq).pipeTo(probe.ref)
    probe.expectMsg(3.seconds, Seq(Seq(1, 2), Seq(3, 4)))
  }

  "example of using Sink.actorRef" in {
    case object Tick
    val sourceUnderTest = Source.tick(0.seconds, 200.millis, Tick)

    case object Completed
    val probe = TestProbe()
    val cancellable =
      sourceUnderTest
        .to(Sink.actorRef(probe.ref, Completed, e => Status.Failure(e)))
        .run()

    probe.expectMsg(1.second, Tick)
    probe.expectNoMessage(100.millis)
    probe.expectMsg(3.seconds, Tick)
    cancellable.cancel()
    probe.expectMsg(3.seconds, Completed)
  }

  "example of using Source.actorRef" in {
    val sinkUnderTest =
      Flow[Int].map(_.toString).toMat(Sink.fold("")(_ + _))(Keep.right)
    val cm: PartialFunction[Any, CompletionStrategy] = { case Done =>
      CompletionStrategy.immediately
    }
    val (ref, future) = Source
      .actorRef(cm, PartialFunction.empty, 8, OverflowStrategy.fail)
      .toMat(sinkUnderTest)(Keep.both)
      .run()

    ref ! 1
    ref ! 2
    ref ! 3
    ref ! akka.actor.Status.Success(CompletionStrategy.draining)

    val result = Await.result(future, 3.seconds)
    result should be("123")
  }

  "example of using TestSink.probe" in {
    val sourceUnderTest = Source(1 to 4).filter(_ % 2 == 0).map(_ * 2)
    sourceUnderTest
      .runWith(TestSink.probe)
      .request(2)
      .expectNext(4, 8)
      .expectComplete()
  }

  "example of using TestSource.probe" in {
    val sinkUnderTest = Sink.cancelled
    TestSource
      .probe[Int]
      .toMat(sinkUnderTest)(Keep.left)
      .run()
      .expectCancellation()
  }

  "example of a exception injection" in {
    val sinkUnderTest = Sink.head[Int]

    val (probe, future) =
      TestSource.probe[Int].toMat(sinkUnderTest)(Keep.both).run()
    probe.sendError(new Exception("boom"))

    Await.ready(future, 3.seconds)
    val exception = future.failed.futureValue
    exception.getMessage shouldBe "boom"
  }

  "example of using TestSource and TestSink for testing flows" in {
    import system.dispatcher
    val flowUnderTest = Flow[Int].mapAsyncUnordered(2) { sleep =>
      pattern.after(10.millis * sleep, using = system.scheduler)(
        Future.successful(sleep)
      )
    }

    val (pub, sub) = TestSource
      .probe[Int]
      .via(flowUnderTest)
      .toMat(TestSink.probe)(Keep.both)
      .run()

    sub.request(3)
    pub.sendNext(3)
    pub.sendNext(2)
    pub.sendNext(1)
    sub.expectNextUnordered(1, 2, 3)

    pub.sendError(new Exception("Power surge in linear subroutine C-47!"))
    val ex = sub.expectError()
    ex.getMessage.contains("C-47") should be(true)

  }

}
