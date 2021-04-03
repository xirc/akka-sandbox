package interaction

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.collection.immutable
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.reflect.ClassTag

object GeneralPurposeResponseAggregatorExample extends App {

  object Hotel1 {

    final case class RequestQuote(replyTo: ActorRef[Quote])

    final case class Quote(hotel: String, price: BigDecimal)

  }

  object Hotel2 {

    final case class RequestPrice(replyTo: ActorRef[Price])

    final case class Price(hotel: String, price: BigDecimal)

  }

  object Aggregator {

    sealed trait Command

    private case object ReceiveTimeout extends Command

    private case class WrappedReply[R](reply: R) extends Command

    def apply[Reply: ClassTag, Aggregate]
    (
      sendRequests: ActorRef[Reply] => Unit,
      expectedReplies: Int,
      replyTo: ActorRef[Aggregate],
      aggregateReplies: immutable.IndexedSeq[Reply] => Aggregate,
      timeout: FiniteDuration
    ): Behavior[Command] = {
      Behaviors.setup { context =>
        context.setReceiveTimeout(timeout, ReceiveTimeout)
        val replyAdapter = context.messageAdapter[Reply](WrappedReply(_))
        sendRequests(replyAdapter)

        def collecting(replies: immutable.IndexedSeq[Reply]): Behavior[Command] = {
          Behaviors.receiveMessage {
            case WrappedReply(reply: Reply) =>
              val newReplies = replies :+ reply
              if (newReplies.size == expectedReplies) {
                val result = aggregateReplies(newReplies)
                replyTo ! result
                Behaviors.stopped
              } else {
                collecting(newReplies)
              }
            case ReceiveTimeout =>
              val aggregate = aggregateReplies(replies)
              replyTo ! aggregate
              Behaviors.stopped
          }
        }

        collecting(Vector.empty)
      }
    }
  }

  object HotelConsumer {

    sealed trait Command

    final case class AggregatedQuote(quotes: List[Quote]) extends Command

    final case class Quote(hotel: String, price: BigDecimal)

    type Reply = Any

    def apply
    (
      hotel1: ActorRef[Hotel1.RequestQuote],
      hotel2: ActorRef[Hotel2.RequestPrice]
    ): Behavior[Command] = {
      Behaviors.setup { context =>
        val aggregator = Aggregator[Reply, AggregatedQuote](
          sendRequests = { replyTo =>
            hotel1 ! Hotel1.RequestQuote(replyTo)
            hotel2 ! Hotel2.RequestPrice(replyTo)
          },
          expectedReplies = 2,
          context.self,
          aggregateReplies = { replies =>
            AggregatedQuote(
              replies.map {
                case Hotel1.Quote(hotel, price) => Quote(hotel, price)
                case Hotel2.Price(hotel, price) => Quote(hotel, price)
              }.sortBy(_.price).toList
            )
          },
          timeout = 5.seconds
        )
        context.spawnAnonymous(aggregator)
        Behaviors.receiveMessage {
          case AggregatedQuote(quotes) =>
            context.log.info("Best {}", quotes.headOption.getOrElse("Quote N/A"))
            Behaviors.same
        }
      }
    }
  }

  object Main {
    def apply(): Behavior[NotUsed] = {
      Behaviors.setup { context =>
        val hotel1 = context.spawnAnonymous(
          Behaviors.receiveMessage[Hotel1.RequestQuote] { message =>
            message.replyTo ! Hotel1.Quote("hotel1", 100)
            Behaviors.same
          }
        )
        val hotel2 = context.spawnAnonymous(
          Behaviors.receiveMessage[Hotel2.RequestPrice] { message =>
            message.replyTo ! Hotel2.Price("hotel2", 80)
            Behaviors.same
          }
        )
        context.spawnAnonymous(HotelConsumer(hotel1, hotel2))
        Behaviors.same
      }
    }
  }

  val system = ActorSystem(Main(), "system")
  Thread.sleep(100)
  system.terminate()
}
