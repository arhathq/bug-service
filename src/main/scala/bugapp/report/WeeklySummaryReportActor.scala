package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}

/**
  * @author Alexander Kuleshov
  */
class WeeklySummaryReportActor(owner: ActorRef) extends Actor with ActorLogging {
  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val data =
        <week-summary-report/>

      owner ! ReportDataResponse(reportId, data)
  }
}

object WeeklySummaryReportActor {
  def props(owner: ActorRef) = Props(classOf[WeeklySummaryReportActor], owner)
}