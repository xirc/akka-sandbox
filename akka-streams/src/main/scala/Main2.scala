import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent._
import scala.concurrent.duration._

final case class Author(handle: String)
final case class Hashtag(name: String)
final case class Tweet(author: Author, timestamp: Long, body: String) {
  def hashtags: Set[Hashtag] =
    body
      .split(" ")
      .collect {
        case t if t.startsWith("#") => Hashtag(t.replaceAll("[^#\\w]", ""))
      }
      .toSet
}

object Main2 extends App {
  implicit val system = ActorSystem("QuickStart2")
  implicit val executionContext = system.dispatcher

  val akkaTag = Hashtag("#akka")

  val tweets: Source[Tweet, NotUsed] = Source(
    Tweet(Author("rolandkuhn"), System.currentTimeMillis, "#akka rocks!") ::
      Tweet(Author("patriknw"), System.currentTimeMillis, "#akka !") ::
      Tweet(Author("bantonsson"), System.currentTimeMillis, "#akka !") ::
      Tweet(Author("drewhk"), System.currentTimeMillis, "#akka !") ::
      Tweet(
        Author("ktosopl"),
        System.currentTimeMillis,
        "#akka on the rocks!"
      ) ::
      Tweet(Author("mmartynas"), System.currentTimeMillis, "wow #akka !") ::
      Tweet(Author("akkateam"), System.currentTimeMillis, "#akka rocks!") ::
      Tweet(Author("bananaman"), System.currentTimeMillis, "#bananas rock!") ::
      Tweet(Author("appleman"), System.currentTimeMillis, "#apples rock!") ::
      Tweet(
        Author("drama"),
        System.currentTimeMillis,
        "we compared #apples to #oranges!"
      ) ::
      Nil
  )

  val authors: Source[Author, NotUsed] =
    tweets.filter(_.hashtags.contains(akkaTag)).map(_.author)

  // --
  println("\nprintAuthors")
  val printAuthorsFuture = authors.runWith(Sink.foreach(println))
  // or
  // authors.runForeach(println)
  Await.result(printAuthorsFuture, Duration.Inf)

  val hashtags: Source[Hashtag, NotUsed] = tweets.mapConcat(_.hashtags)

  // --
  println("\nprintHashtags")
  val printHashtagsFuture = hashtags.runForeach(println)
  Await.result(printHashtagsFuture, Duration.Inf)

  // --
  println("\nuse Materialized Values")
  val count: Flow[Tweet, Int, NotUsed] = Flow[Tweet].map(_ => 1)
  val sumSink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)
  val counterGraph: RunnableGraph[Future[Int]] =
    tweets.via(count).toMat(sumSink)(Keep.right)
  val sumFuture: Future[Int] = counterGraph.run()
  val sum = Await.result(sumFuture, Duration.Inf)
  println(s"Total tweets processed: $sum")

  // --
  // Graph DSL
  val writeAuthors: Sink[Author, NotUsed] = ???
  val writeHashTags: Sink[Hashtag, NotUsed] = ???
  val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._
    val bcast = builder.add(Broadcast[Tweet](2))
    tweets ~> bcast.in
    bcast.out(0) ~> Flow[Tweet].map(_.author) ~> writeAuthors
    bcast.out(1) ~> Flow[Tweet].mapConcat(_.hashtags) ~> writeHashTags
    ClosedShape
  })
  println("\nrun graph")
  g.run()

}
