import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.AskTimeoutException
import akka.testkit.{TestActors, TestKit, TestProbe}
import akka.util.Timeout
import akka.stream._
import akka.stream.scaladsl._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent._
import scala.concurrent.duration._

final class ActorInteropSpec
    extends TestKit(ActorSystem("actor-system"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {
  "example of ask" in {
    implicit val askTimeout = Timeout(1.seconds)

    val words: Source[String, NotUsed] =
      Source(List("hello", "hi"))

    val echoActorRef =
      system.actorOf(TestActors.echoActorProps)

    val resultFuture =
      words
        .ask[String](parallelism = 5)(echoActorRef)
        .map(_.toUpperCase)
        .runWith(Sink.seq)

    val result = Await.result(resultFuture, 3.seconds)
    result shouldBe Seq("HELLO", "HI")

    watch(echoActorRef)
    system.stop(echoActorRef)
    expectTerminated(echoActorRef)

    val failureFuture =
      words
        .ask[String](parallelism = 5)(echoActorRef)
        .map(_.toUpperCase)
        .runWith(Sink.seq)

    the[AskTimeoutException] thrownBy {
      Await.result(failureFuture, 3.seconds)
    }
  }

  "example of sink.actorRefWithBackpressure" in {
    object AckingReciver {
      case object Ack
      case object StreamInitialized
      case object StreamCompleted
      final case class StreamFailure(ex: Throwable)
      def props(probe: ActorRef, ackWith: Any): Props =
        Props(new AckingReciver(probe, ackWith))
    }
    class AckingReciver(probe: ActorRef, ackWith: Any)
        extends Actor
        with ActorLogging {
      import AckingReciver._

      override def receive: Receive = {
        case StreamInitialized =>
          log.info("Stream initialized!")
          probe ! "Stream initialized!"
          sender() ! Ack
        case el: String =>
          log.info("Received element: {}", el)
          probe ! el
          sender() ! Ack
        case StreamCompleted =>
          log.info("Stream completed!")
          probe ! "Stream completed!"
        case StreamFailure(ex) =>
          log.error(ex, "Stream failed!")
      }
    }

    val words: Source[String, NotUsed] =
      Source(List("hello", "hi"))

    val probe = TestProbe()
    val receiver =
      system.actorOf(AckingReciver.props(probe.ref, AckingReciver.Ack))
    val sink = Sink.actorRefWithBackpressure(
      receiver,
      AckingReciver.StreamInitialized,
      AckingReciver.Ack,
      AckingReciver.StreamCompleted,
      AckingReciver.StreamFailure
    )

    words.map(_.toLowerCase).runWith(sink)
    probe.expectMsg("Stream initialized!")
    probe.expectMsg("hello")
    probe.expectMsg("hi")
    probe.expectMsg("Stream completed!")
  }

  "example of Source.queue" in {
    val queue =
      Source
        .queue[Int](10, OverflowStrategy.backpressure)
        .throttle(2, 1.seconds)
        .map(x => x * x)
        .toMat(Sink.foreach(x => println(s"completed $x")))(Keep.left)
        .run()

    val source = Source(1 to 10)

    import system.dispatcher
    val future = source
      .mapAsync(1)(x => {
        queue.offer(x).map {
          case QueueOfferResult.Enqueued => println(s"enqueued $x")
          case QueueOfferResult.Dropped  => println(s"dropped $x")
          case QueueOfferResult.Failure(ex) =>
            println(s"Offer failed ${ex.getMessage}")
          case QueueOfferResult.QueueClosed => println("Source Queue closed")
        }
      })
      .runWith(Sink.ignore)
    Await.result(future, 5.seconds)
  }

  "example of Source.actorRef" in {
    val cm: PartialFunction[Any, CompletionStrategy] = { case Done =>
      CompletionStrategy.immediately
    }
    val ref = Source
      .actorRef[Int](
        cm,
        PartialFunction.empty,
        10,
        OverflowStrategy.fail
      ) // backpressure is not supported.
      .map(x => x * x)
      .toMat(Sink.foreach(x => println(s"completed $x")))(Keep.left)
      .run()

    ref ! 1
    ref ! 2
    ref ! 3
    ref ! akka.actor.Status.Success("Done")
    Thread.sleep(1.seconds.toMillis)
  }
}
