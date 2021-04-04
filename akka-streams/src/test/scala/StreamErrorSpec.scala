import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl._

final class StreamErrorSpec extends BaseSpec(ActorSystem("stream-error-spec")) {

  "example of logging error" in {
    val future = Source(-5 to 5)
      .map(1 / _)
      .log("error logging")
      .runWith(Sink.ignore)
    future.failed.futureValue shouldBe a[ArithmeticException]
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
    future.futureValue shouldBe Done
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
    future.futureValue shouldBe Done
  }

}
