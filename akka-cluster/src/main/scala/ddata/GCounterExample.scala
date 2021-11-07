package ddata

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.ddata.GCounterKey
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import util.ActorSystemFactory

import scala.concurrent.duration.DurationInt
import scala.util.Random

object GCounterExample extends App {
  object Guardian {
    def apply(): Behavior[Counter.Command] = Counter(GCounterKey("counter-key"))
  }

  implicit val timeout: Timeout = 3.seconds
  implicit val mainSystem: ActorSystem[Counter.Command] =
    ActorSystem(Guardian(), "system")

  // Use multiple ActorSystems in Single JVM for clustering multiple nodes easily.
  val systems = (0 to 2).map { _ =>
    ActorSystemFactory.createWithRandomPort(Guardian(), "system")
  }

  val route: Route = {
    path("counter") {
      val system = Random.shuffle(systems).headOption.getOrElse(mainSystem)
      concat(
        parameter("cache".as[Boolean].withDefault(true)) { useCache =>
          get {
            val response = if (useCache) {
              system.ask(Counter.GetCachedValue)
            } else {
              system.ask(Counter.GetValue)
            }
            onComplete(response) { value =>
              complete(value.toString)
            }
          }
        },
        post {
          system ! Counter.Increment
          complete(StatusCodes.OK)
        }
      )
    }
  }
  Http().newServerAt("127.0.0.1", 8080).bind(route)
}
