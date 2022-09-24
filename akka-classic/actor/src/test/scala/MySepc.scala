import akka.actor.ActorSystem
import akka.pattern.pipe
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future
import scala.concurrent.duration._

class MySepc
    extends TestKit(ActorSystem("my-spec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {
  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "assertion example" must {
    val echo = system.actorOf(TestActors.echoActorProps)

    "expectMsg" in {
      echo ! "hello world"
      expectMsg("hello world")
    }

    "expectMsgAnyOf" in {
      echo ! "world"
      val any = expectMsgAnyOf("hello", "world")
      any mustBe "world"
    }

    "expectMsgAnyOf2" in {
      echo ! Some(1)
      val any = expectMsgAnyOf(None, Some(1), Some(2))
      any mustBe Some(1)
    }

    "expectMsgAllOf" in {
      echo ! "hello"
      echo ! "world"
      expectMsgAllOf("hello", "world")
    }

    "expectMsgAllOf2" in {
      echo ! Some(1)
      echo ! None
      expectMsgAllOf(None, Some(1))
    }

    "expectMsgType" in {
      echo ! 1
      val i = expectMsgType[Int]
      i mustBe 1
    }

    "expectMsgType2" in {
      sealed trait Response
      case class Successful() extends Response
      case class Failure() extends Response

      echo ! Successful()
      expectMsgType[Successful]

      echo ! Failure()
      expectMsgType[Failure]

      echo ! Successful()
      val successful = expectMsgType[Response]
      successful mustBe Successful()
    }

    "expectNoMessage" in {
      expectNoMessage(200.millis)
    }

    "receiveN" in {
      echo ! "abc"
      echo ! 123
      val two = receiveN(2)
      two(0) mustBe "abc"
      two(1) mustBe 123
    }

    "expectMsgPF" in {
      echo ! 123
      val strmsg = expectMsgPF() { case value: Int =>
        value.toString
      }
      strmsg mustBe "123"
    }

    "expectMsgClass" in {
      sealed trait A
      case class B() extends A

      val value = B()

      echo ! value
      val expectA = expectMsgClass(classOf[A])
      expectA mustBe value

      echo ! value
      val expectB = expectMsgClass(classOf[B])
      expectB mustBe value
    }

    "expectMsgAnyClassOf" in {
      sealed trait Response
      case class Successful() extends Response
      case class Failure() extends Response

      echo ! Failure()
      val response = expectMsgAnyClassOf(classOf[Response], classOf[Successful])
      response mustBe Failure()

      echo ! Successful()
      val successful = expectMsgAnyClassOf(classOf[Successful])
      successful mustBe Successful()
    }

    "expectMsgAllClassOf" in {
      echo ! "123"
      echo ! 123
      echo ! Some(123)
      // the following statement cannot capture Option[Int]
      val all =
        expectMsgAllClassOf[Any](
          classOf[Int],
          classOf[String],
          classOf[Some[Int]]
        )
      all(0) mustBe "123"
      all(1) mustBe 123
      all(2) mustBe Some(123)
    }

    "expectMsgAllConformingOf" in {
      echo ! "123"
      echo ! 123
      echo ! Some(123)
      val all = expectMsgAllConformingOf[Any](
        classOf[Int],
        classOf[String],
        classOf[Option[Int]]
      )
      all(0) mustBe "123"
      all(1) mustBe 123
      all(2) mustBe Some(123)
    }

    "fishForMessage" in {
      echo ! "annoying"
      echo ! "ignore"
      echo ! "target"
      val fish = fishForMessage(1.seconds) {
        case "annoying" => false
        case "ignore"   => false
        case _          => true
      }
      fish mustBe "target"
    }

    "receiveOne" in {
      echo ! 1
      val msg = receiveOne(1.seconds)
      msg mustBe 1
    }

    "receiveWhile" in {
      import system.dispatcher
      akka.pattern.after(1.seconds)(Future.successful(1)).pipeTo(echo)
      akka.pattern.after(3.seconds)(Future.successful(2)).pipeTo(echo)
      val messages = receiveWhile(4.seconds, 1.5.seconds, 2) { case value =>
        value
      }
      messages.size mustBe 1
      messages(0) mustBe 1
      expectMsg(2)
    }

    "awaitCond" in {
      import system.dispatcher
      var mutable: Option[Int] = None
      akka.pattern.after(0.5.seconds)(Future.successful(1)).foreach { value =>
        mutable = None
      }
      akka.pattern.after(1.seconds)(Future.successful(2)).foreach { value =>
        mutable = Some(value)
      }
      awaitCond(mutable.isDefined, 3.seconds)
    }

    "awaitAssert" in {
      import system.dispatcher
      var mutable: Option[Int] = None
      akka.pattern.after(0.5.seconds)(Future.successful(1)).foreach { value =>
        mutable = Some(value)
      }
      akka.pattern.after(1.seconds)(Future.successful(2)).foreach { value =>
        mutable = Some(value)
      }
      awaitAssert({ mutable mustBe Some(2) }, 2.seconds, 0.5.seconds)
    }

    "ignoreMsg" in {
      ignoreMsg {
        case value: Int    => true
        case value: String => false
      }
      echo ! 1
      echo ! 2
      echo ! "3"
      echo ! 4
      expectMsg("3")
      expectNoMessage()
    }

  }
}
