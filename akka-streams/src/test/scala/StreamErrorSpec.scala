import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent._
import scala.concurrent.duration._

final class StreamErrorSpec
    extends TestKit(ActorSystem("stream-error-spec"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {
  "example of logging error" in {
    val future = Source(-5 to 5)
      .map(1 / _)
      .log("error logging")
      .runWith(Sink.ignore)
    Await.ready(future, 1.seconds)
  }

  "example of recover" in {
    val future = Source(0 to 6)
      .map(n => {
        if (List(4, 5).contains(n))
          throw new RuntimeException(s"Boom! Bad value found: $n")
        else n.toString
      })
      .recover { case e: RuntimeException =>
        e.getMessage
      }
      .runForeach(println)
    Await.ready(future, 1.seconds)
    future.value.get.isSuccess shouldBe true
  }

  "example of recoverWithRetries" in {
    val planB = Source(List("five", "six", "seven", "eight"))
    val future = Source(0 to 10)
      .map(n => {
        if (n < 5) n.toString
        else throw new RuntimeException("Boom!")
      })
      .recoverWithRetries(
        attempts = 1,
        { case _: RuntimeException =>
          planB
        }
      )
      .runForeach(println)
    Await.ready(future, 1.seconds)
    future.value.get.isSuccess shouldBe true
  }
}
