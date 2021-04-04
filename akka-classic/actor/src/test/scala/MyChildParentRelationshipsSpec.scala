import akka.actor.{Actor, ActorRef, ActorRefFactory, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.annotation.nowarn

class MyChildParentRelationshipsSpec
    extends TestKit(ActorSystem("test-system"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  class Parent extends Actor {
    val child = context.actorOf(Props[Child]())
    var ponged = false

    override def receive: Receive = {
      case "pingit" => child ! "pong"
      case "pong"   => ponged = true
    }
  }
  class Child extends Actor {
    override def receive: Receive = { case "ping" =>
      context.parent ! "pong"
    }
  }

  "use explicit reference" in {
    class DependentChild(parent: ActorRef) extends Actor {
      override def receive: Receive = { case "ping" =>
        parent ! "pong"
      }
    }
    val child = system.actorOf(Props(new DependentChild(testActor)))
    child ! "ping"
    expectMsg("pong")
  }

  "create the child using TestProbe" in {
    val parent = TestProbe()
    val child = parent.childActorOf(Props(new Child()))
    parent.send(child, "ping")
    parent.expectMsg("pong")
  }

  "using fabricated parent" in {
    val proxy = TestProbe()
    val parent = system.actorOf(Props(new Actor {
      val child = context.actorOf(Props(new Child), "child")
      override def receive: Receive = {
        case x if sender() == child => proxy.ref.forward(x)
        case x                      => child.forward(x)
      }
    }))

    proxy.send(parent, "ping")
    proxy.expectMsg("pong")
  }

  "using ActorRefFactory" in {
    class DependentParent(childProps: Props, probe: ActorRef) extends Actor {
      val child = context.actorOf(childProps, "child")
      override def receive: Receive = {
        case "pingit" => child ! "ping"
        case "pong"   => probe ! "ponged"
      }
    }
    class GenericDependentParent(childMaker: ActorRefFactory => ActorRef)
        extends Actor {
      val child = childMaker(context)
      var ponged = false
      override def receive: Receive = {
        case "pingit" => child ! "ping"
        case "pong"   => ponged = true
      }
    }

    val probe = TestProbe()
    val maker = (_: ActorRefFactory) => probe.ref
    val parent = system.actorOf(Props(new GenericDependentParent(maker)))
    parent ! "pingit"
    probe.expectMsg("ping")

    // in your application code
    val maker2 = (f: ActorRefFactory) => f.actorOf(Props(new Child))
    @nowarn
    val parent2 = system.actorOf(Props(new GenericDependentParent(maker2)))
  }

}
