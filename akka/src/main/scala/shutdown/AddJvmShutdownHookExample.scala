package shutdown

import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory

object AddJvmShutdownHookExample extends App {
  val config = ConfigFactory.parseString("""
      |akka.coordinated-shutdown.run-by-jvm-shutdown-hook = on
      |""".stripMargin)
  val system = ActorSystem(MyActor(), "system", config)
  CoordinatedShutdown(system).addJvmShutdownHook {
    println("JVM shutdown hook")
  }
  object MyShutdownReason extends CoordinatedShutdown.Reason
  CoordinatedShutdown(system).runAll(MyShutdownReason)
}
