import akka.NotUsed
import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.stream._
import akka.stream.scaladsl._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent._
import scala.concurrent.duration._

final class DynamicStreamHandlingSpec
    extends TestKit(ActorSystem())
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val countingSource =
    Source
      .fromIterator(() => Iterator.from(1))
      .delay(1.second, DelayOverflowStrategy.backpressure)
      .throttle(1, 1.second)
  val lastSink = Sink.last[Int]

  "example of unique kill switch (shutdown)" in {
    val (killSwitch, last) =
      countingSource
        .viaMat(KillSwitches.single)(Keep.right)
        .toMat(lastSink)(Keep.both)
        .run()

    Thread.sleep(2.5.seconds.toMillis)
    killSwitch.shutdown()
    Await.result(last, 1.second) shouldBe 2
  }

  "example of unique kill switch (abort)" in {
    val (killSwitch, last) =
      countingSource
        .viaMat(KillSwitches.single)(Keep.right)
        .toMat(lastSink)(Keep.both)
        .run()

    val error = new RuntimeException("boom!")
    killSwitch.abort(error)

    Await.result(last.failed, 1.second) shouldBe error
  }

  "example of shared kill switch (shutdown)" in {
    val sharedKillSwitch = KillSwitches.shared("my-kill-switch")

    val last =
      countingSource
        .via(sharedKillSwitch.flow)
        .runWith(lastSink)

    val delayedLast =
      countingSource
        .delay(1.second)
        .via(sharedKillSwitch.flow)
        .runWith(lastSink)

    Thread.sleep(2.5.seconds.toMillis)

    sharedKillSwitch.shutdown()

    Await.result(last, 1.second) shouldBe 2
    Await.result(delayedLast, 1.second) shouldBe 1
  }

  "example of shared kill switch (abort)" in {
    val sharedKillSwitch = KillSwitches.shared("my-kill-switch")

    val last1 = countingSource.via(sharedKillSwitch.flow).runWith(lastSink)
    val last2 = countingSource.via(sharedKillSwitch.flow).runWith(lastSink)

    val error = new RuntimeException("boom!")
    sharedKillSwitch.abort(error)

    Await.result(last1.failed, 1.second) shouldBe error
    Await.result(last2.failed, 1.second) shouldBe error
  }

  "example of MergeHub" in {
    val consumer = Sink.foreach(println)

    val runnableGraph: RunnableGraph[Sink[String, NotUsed]] =
      MergeHub.source[String](perProducerBufferSize = 16).to(consumer)

    val toConsumer: Sink[String, NotUsed] = runnableGraph.run()

    Source.single("Hello!").runWith(toConsumer)
    Source.single("Hub!").runWith(toConsumer)
  }

  "example of BroadcastHub" in {
    val producer = Source.tick(1.second, 1.second, "New message")

    val runnableGraph: RunnableGraph[Source[String, NotUsed]] =
      producer.toMat(BroadcastHub.sink(bufferSize = 256))(Keep.right)

    val fromProducer: Source[String, NotUsed] = runnableGraph.run()

    val future1 =
      fromProducer.take(3).runForeach(msg => println(s"consumer1: $msg"))
    val future2 =
      fromProducer.take(3).runForeach(msg => println(s"consumer2: $msg"))

    Await.ready(future1, 5.seconds)
    Await.ready(future2, 5.seconds)
  }

  "example of simple Pub-Sub service" in {
    val (sink, source) =
      MergeHub
        .source[String](perProducerBufferSize = 16)
        .toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)
        .run()

    // Ensure that the broadcast output is dropped if there are no listening parties.
    source.runWith(Sink.ignore)

    val busFlow: Flow[String, String, UniqueKillSwitch] =
      Flow
        .fromSinkAndSource(sink, source)
        .joinMat(KillSwitches.singleBidi[String, String])(Keep.right)
        .backpressureTimeout(3.seconds)

    val (switch, future) =
      Source
        .repeat("Hello World!")
        .viaMat(busFlow)(Keep.right)
        .toMat(Sink.foreach(println))(Keep.both)
        .run()

    Thread.sleep(2.seconds.toMillis)
    switch.shutdown()
    Await.ready(future, 3.seconds)
  }

  "example of PartitionHub" in {
    val producer = Source
      .tick(100.millis, 100.millis, "message")
      .zipWith(Source(1 to 100))((a, b) => s"$a-$b")
    val runnableGraph: RunnableGraph[Source[String, NotUsed]] =
      producer
        .toMat(
          PartitionHub.sink(
            (size, elem) => math.abs(elem.hashCode % size),
            startAfterNrOfConsumers = 2,
            bufferSize = 256
          )
        )(Keep.right)

    val fromProducer = runnableGraph.run()
    val future1 = fromProducer.runForeach(msg => println(s"consumer1: $msg"))
    val future2 = fromProducer.runForeach(msg => println(s"consumer2: $msg"))
    Await.ready(future1, 15.seconds)
    Await.ready(future2, 15.seconds)
  }

  "example of PartitionHub (stateful)" in {
    val producer = Source
      .tick(100.millis, 100.millis, "message")
      .zipWith(Source(1 to 100))((a, b) => s"$a-$b")

    def roundRobin(): (PartitionHub.ConsumerInfo, String) => Long = {
      var i = -1L
      (info, elem) => {
        i += 1
        info.consumerIdByIdx((i % info.size).toInt)
      }
    }

    val runnableGraph =
      producer.toMat(
        PartitionHub.statefulSink(
          roundRobin,
          startAfterNrOfConsumers = 2,
          bufferSize = 256
        )
      )(Keep.right)

    val fromProducer = runnableGraph.run()

    val future1 = fromProducer.runForeach(msg => println(s"consumer1: $msg"))
    val future2 = fromProducer.runForeach(msg => println(s"consumer2: $msg"))
    Await.ready(future1, 15.seconds)
    Await.ready(future2, 15.seconds)
  }

  "example of PartitionHub (with queueSize)" in {
    val producer = Source(0 until 100)

    def balanced[T](): (PartitionHub.ConsumerInfo, T) => Long = {
      (info, elem) =>
        {
          info.consumerIds.minBy(id => info.queueSize(id))
        }
    }

    val runnableGraph =
      producer.toMat(
        PartitionHub.statefulSink(
          balanced,
          startAfterNrOfConsumers = 2,
          bufferSize = 15
        )
      )(Keep.right)

    val fromProducer = runnableGraph.run()

    val future1 = fromProducer
      .throttle(2, 50.millis)
      .runForeach(msg => println(s"consumer1: $msg"))
    val future2 = fromProducer
      .throttle(10, 100.millis)
      .runForeach(msg => println(s"consumer2: $msg"))
    Await.ready(future1, 10.seconds)
    Await.ready(future2, 10.seconds)
  }

}
