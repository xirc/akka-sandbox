import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import com.typesafe.config.{Config, ConfigFactory}

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.immutable
import scala.concurrent._
import scala.concurrent.duration._

object FutureInteropSpec {
  def config: Config =
    ConfigFactory.parseString("""
        |thread-pool-dispatcher {
        |  executor = "thread-pool-executor"
        |}
        |fork-join-dispatcher {
        |  executor = "fork-join-executor"
        |}
        |
        |""".stripMargin)
}
final class FutureInteropSpec
    extends BaseSpec(ActorSystem("actor-system", FutureInteropSpec.config)) {

  "example of mapAsync" in {
    case class Tweet(author: Author, hashtags: Set[String])
    case class Author(handle: String)
    case class Email(to: EmailAddress, title: String, body: String)
    case class EmailAddress(value: String)

    object EmailService {
      implicit val executionContext =
        system.dispatchers.lookup("thread-pool-dispatcher")
      def send(email: Email): Future[Unit] = Future {
        Thread.sleep(500)
        println(s"send $email")
      }
    }

    object EmailAddressLookupService {
      implicit val executionContext =
        system.dispatchers.lookup("fork-join-dispatcher")
      def lookup(handle: String): Future[Option[EmailAddress]] = Future {
        Thread.sleep(500)
        Some(EmailAddress(s"${handle.trim}@example.com"))
      }
    }

    val tweets: Source[Tweet, NotUsed] =
      Source(
        immutable.Seq(
          Tweet(Author("john"), Set("akka", "scala")),
          Tweet(Author("lena"), Set("java")),
          Tweet(Author("mikel"), Set("python"))
        )
      )

    val authors: Source[Author, NotUsed] =
      tweets.filter(_.hashtags.contains("akka")).map(_.author)

    val emailAddresses: Source[EmailAddress, NotUsed] =
      authors
        .mapAsync(4)(author => EmailAddressLookupService.lookup(author.handle))
        .collect { case Some(address) => address }

    val sendEmails: RunnableGraph[NotUsed] =
      emailAddresses
        .mapAsync(4)(address => {
          EmailService.send(
            Email(to = address, title = "Akka", body = "I like your tweet")
          )
        })
        .to(Sink.ignore)

    sendEmails.run()
    Thread.sleep(3000)
  }

  "example of difference between mapAsync and mapAsyncUnordered" in {
    class SometimesSlowService(implicit executionContext: ExecutionContext) {
      private val runningCount = new AtomicInteger
      def convert(s: String): Future[String] = {
        println(s"running: $s (${runningCount.incrementAndGet()})")
        Future {
          if (s.nonEmpty && s.head.isLower) {
            Thread.sleep(500)
          } else {
            Thread.sleep(20)
          }
          println(s"completed: $s (${runningCount.decrementAndGet()})")
          s.toUpperCase
        }
      }
    }

    implicit val ec = system.dispatchers.lookup("thread-pool-dispatcher")
    val service = new SometimesSlowService

    println("\nmapAsync\n")
    val source = Source(List("a", "B", "C", "D", "e", "F", "g", "H", "i", "J"))
    val futureAsync = source
      .map(e => { println(s"before: $e"); e })
      .mapAsync(4)(service.convert)
      .toMat(Sink.foreach(e => println(s"after: $e")))(Keep.right)
      .withAttributes(Attributes.inputBuffer(initial = 4, max = 4))
      .run()
    Await.ready(futureAsync, 10.seconds)

    println("\nmapAsyncUnordered\n")
    val futureAsyncUnordered = source
      .map(e => { println(s"before: $e"); e })
      .mapAsyncUnordered(4)(service.convert)
      .toMat(Sink.foreach(e => println(s"after: $e")))(Keep.right)
      .withAttributes(Attributes.inputBuffer(initial = 4, max = 4))
      .run()
    Await.ready(futureAsyncUnordered, 10.seconds)
  }

}
