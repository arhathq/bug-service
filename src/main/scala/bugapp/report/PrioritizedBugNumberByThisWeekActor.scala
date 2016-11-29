package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.report.ReportDataBuilder.{ReportData, ReportDataRequest, ReportDataResponse}

/**
  * @author Alexander Kuleshov
  */
class PrioritizedBugNumberByThisWeekActor(owner: ActorRef) extends Actor with ActorLogging {
  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val data =
        <priority-bugs-by-this-week/>

      owner ! ReportDataResponse(ReportData(reportId, reportParams(ReportParams.ReportType).asInstanceOf[String], data))
  }
}

object PrioritizedBugNumberByThisWeekActor {
  def props(owner: ActorRef) = Props(classOf[PrioritizedBugNumberByThisWeekActor], owner)
}