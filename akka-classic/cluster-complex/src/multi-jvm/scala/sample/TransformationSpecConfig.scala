package sample

import akka.remote.testkit.MultiNodeConfig
import com.typesafe.config.ConfigFactory

object TransformationSpecConfig extends MultiNodeConfig {
  val seed = role("seed")
  val frontend = role("frontend")
  val backend = role("backend")

  commonConfig(ConfigFactory.parseString(
    """
      |akka.actor.provider = cluster
      |akka.actor.allow-java-serialization = on
      |""".stripMargin)
  )
  nodeConfig(seed) {
    ConfigFactory.parseString(
      """
        |akka.cluster.roles = [ "seed" ]
        |""".stripMargin)
  }
  nodeConfig(frontend) {
    ConfigFactory.parseString(
      """
        |akka.cluster.roles = [ "frontend" ]
        |""".stripMargin)
  }
  nodeConfig(backend) {
    ConfigFactory.parseString(
      """
        |akka.cluster.roles = [ "backend" ]
        |""".stripMargin
    )
  }
}
