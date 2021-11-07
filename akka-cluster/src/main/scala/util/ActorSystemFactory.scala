package util

import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.ConfigFactory

object ActorSystemFactory {
  def createWithRandomPort[T](
      guardianBehavior: Behavior[T],
      name: String
  ): ActorSystem[T] = {
    ActorSystem(
      guardianBehavior,
      name,
      ConfigFactory
        .parseString("akka.remote.artery.canonical.port=0")
        .withFallback(ConfigFactory.load)
    )
  }
}
