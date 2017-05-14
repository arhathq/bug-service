package bugapp.report

import akka.actor.{Actor, ActorRef}
import bugapp.report.ReportDataBuilderActor.ReportDataRequest

/**
  *
  */
abstract class ReportWorker (owner: ActorRef) extends Actor {
  import ReportWorker._

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)

    message match {
      case Some(msg) => msg match {
        case ReportDataRequest(reportId, _, _) =>
          owner ! WorkFailed(reportId, reason.getMessage)
      }
      case None =>
    }
  }
}
object ReportWorker {
  case class WorkFailed(workId: String, message: String)
}