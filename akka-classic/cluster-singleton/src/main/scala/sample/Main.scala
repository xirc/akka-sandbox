package sample

import akka.actor.{ActorSystem, PoisonPill}
import akka.cluster.singleton.{
  ClusterSingletonManager,
  ClusterSingletonManagerSettings,
  ClusterSingletonProxy,
  ClusterSingletonProxySettings
}

import scala.concurrent.duration._

object Main extends App {
  val system = ActorSystem("ClusterSingleton")

  // Singleton Manager /user/consumer
  // Singleton Actor '/user/consumer/singleton' is only in an oldest node
  val manager = system.actorOf(
    ClusterSingletonManager.props(
      Consumer.props,
      terminationMessage = PoisonPill,
      settings = ClusterSingletonManagerSettings(system)
    ),
    name = "consumer"
  )

  // Proxy for Singleton Actor /user/consumerProxy
  val proxy = system.actorOf(
    ClusterSingletonProxy.props(
      "/user/consumer",
      settings = ClusterSingletonProxySettings(system)
    ),
    name = "consumerProxy"
  )

  val pingInterval =
    system.settings.config
      .getDuration("application.consumer-client.ping-interval", MILLISECONDS)
      .millis
  system.actorOf(ConsumerClient.props(proxy, pingInterval))
}
