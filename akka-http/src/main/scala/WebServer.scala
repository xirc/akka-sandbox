import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, get, path}

import scala.io.StdIn

// https://doc.akka.io/docs/akka-http/current/introduction.html
object WebServer extends App {
  implicit val system = ActorSystem("my-system")
  implicit val executionContext = system.dispatcher

  val route =
    path("hello") {
      get {
        complete(
          HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            "<h1>Say hello to akka-http</h1>"
          )
        )
      }
    }

  val binding = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  binding
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())

}
