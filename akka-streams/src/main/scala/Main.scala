import java.nio.file.Paths

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent._
import scala.concurrent.duration._

object Main extends App {
  implicit val system = ActorSystem("QuickStart")
  implicit val executionContext = system.dispatcher

  val source: Source[Int, NotUsed] = Source(1 to 100)
  val done: Future[Done] = source.runForeach(i => println(i))
  Await.result(done, Duration.Inf)

  val factorials = source.scan(BigInt(1))((acc, next) => acc * next)
  def lineSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String]
      .map(s => ByteString(s + "\n"))
      .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)
  val result: Future[IOResult] =
    factorials.map(_.toString).runWith(lineSink("factorial2.txt"))
  Await.result(result, Duration.Inf)

  val result2 = factorials
    .zipWith(Source(0 to 100))((num, idx) => s"$idx! = $num")
    .throttle(1, 1.second)
    .runForeach(println)
  Await.result(result2, Duration.Inf)

  system.terminate()
}
