import akka.actor.{Actor, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}

object SimpleRouterExample extends App {
  final case class Work(msg: String)
  final class Master extends Actor {
    var router: Router = {
      val routees = Vector.fill(5) {
        val r = context.actorOf(Worker.props)
        context.watch(r)
        ActorRefRoutee(r)
      }
      Router(RoundRobinRoutingLogic(), routees)
    }
    override def receive: Receive = {
      case w: Work =>
        router.route(w, sender())
      case Terminated(worker) =>
        router = router.removeRoutee(worker)
        val r = context.actorOf(Worker.props)
        context.watch(r)
        router = router.addRoutee(r)
    }
  }

  val system = ActorSystem("system")

  val master = system.actorOf(Props(new Master))
  master ! Work("Hello")
  master ! Work("World")

  Thread.sleep(1000)

  system.terminate()
}
