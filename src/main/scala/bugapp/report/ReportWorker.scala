package bugapp.report

import akka.actor.{Actor, ActorRef}
import bugapp.report.ReportDataBuilder.ReportDataRequest

/**
  *
  */
abstract class ReportWorker (owner: ActorRef) extends Actor {
  import ReportWorker._

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)

    message.getOrElse(None) match {
      case ReportDataRequest(reportId, _, _) =>
        owner ! WorkFailed(reportId, reason.getMessage)
    }
  }
}
object ReportWorker {
  case class WorkFailed(workId: String, message: String)
}