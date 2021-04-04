package sample

object Messages {
  case object GetStatus
  sealed trait GetStatusResponse
  final case object Ready extends GetStatusResponse
  final case object NotReady extends GetStatusResponse

  final case class TransformationJob(text: String)
  final case class TransformationResult(text: String)
  final case class JobFailed(reason: String, job: TransformationJob)

  case object BackendRegistration
}
