package sharding

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity,
  EntityTypeKey
}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import util.ActorSystemFactory

import scala.concurrent.duration.DurationInt
import scala.util.Random

object ClusterShardingExample extends App {
  object Guardian {
    val TypeKey = EntityTypeKey[Counter.Command]("counter")
    def apply(): Behavior[ShardingEnvelope[Counter.Command]] = Behaviors.setup {
      context =>
        val clusterSharding = ClusterSharding(context.system)
        val shardRegion =
          clusterSharding.init(Entity(TypeKey) { context =>
            Counter(context.entityId)
          })
        Behaviors.receiveMessage { message =>
          shardRegion ! message
          Behaviors.same
        }
    }
  }

  implicit val timeout: Timeout = 3.seconds
  implicit val mainSystem: ActorSystem[ShardingEnvelope[Counter.Command]] =
    ActorSystem(Guardian(), "system")

  // Use multiple ActorSystems in Single JVM for clustering multiple nodes easily.
  val systems = (0 to 2).map { _ =>
    ActorSystemFactory.createWithRandomPort(Guardian(), "system")
  }

  val route: Route = {
    path("counter" / Segment) { id =>
      val system = Random.shuffle(systems).headOption.getOrElse(mainSystem)
      val entityId = URLEncoder.encode(id, StandardCharsets.UTF_8.toString)
      concat(
        get {
          val response = system.ask[Int](askReplyTo =>
            ShardingEnvelope(entityId, Counter.GetValue(askReplyTo))
          )
          onComplete(response) { value =>
            complete(value.toString)
          }
        },
        post {
          system ! ShardingEnvelope(entityId, Counter.Increment)
          complete(StatusCodes.OK)
        }
      )
    }
  }
  Http().newServerAt("127.0.0.1", 8080).bind(route)
}
