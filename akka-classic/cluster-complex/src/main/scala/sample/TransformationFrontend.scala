package sample

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

object TransformationFrontend {
  def props: Props = Props(new TransformationFrontend)
}
class TransformationFrontend extends Actor with ActorLogging {
  import Messages._

  private var backends: IndexedSeq[ActorRef] = IndexedSeq.empty
  private var jobCounter: Int = 0

  log.info("frontend")

  override def receive: Receive = {
    case GetStatus =>
      val status = if (backends.isEmpty) Ready else NotReady
      sender() ! status

    case job: TransformationJob if backends.isEmpty =>
      sender() ! JobFailed("Service unavailable, try again later", job)

    case job: TransformationJob =>
      jobCounter += 1
      backends(jobCounter % backends.size) forward job

    case BackendRegistration if !backends.contains(sender()) =>
      log.info("BackendRegistration {}", sender().path)
      context.watch(sender())
      backends = backends :+ sender()

    case Terminated(backend) =>
      log.info("Terminated {}", backend.path)
      backends = backends.filterNot(_ == backend)
  }
}
