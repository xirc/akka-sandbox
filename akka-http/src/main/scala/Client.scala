import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Client {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val executionContext = system.dispatcher

    val responseFuture: Future[HttpResponse] =
      Http().singleRequest(HttpRequest(uri = "https://akka.io"))

    responseFuture
      .onComplete {
        case Success(res) => println(res)
        case Failure(_)   => sys.error("something wrong")
      }
  }
}
