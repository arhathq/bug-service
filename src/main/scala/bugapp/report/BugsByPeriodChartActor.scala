package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}

/**
  * @author Alexander Kuleshov
  */
class BugsByPeriodChartActor(owner: ActorRef) extends Actor with ActorLogging {
  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val data =
        <bugs-by-15-weeks/>

      owner ! ReportDataResponse(reportId, data)
  }
}

object BugsByPeriodChartActor {
  def props(owner: ActorRef) = Props(classOf[BugsByPeriodChartActor], owner)
}
