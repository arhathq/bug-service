package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}

/**
  * @author Alexander Kuleshov
  */
class ReportersBugNumberByThisWeekActor(owner: ActorRef) extends Actor with ActorLogging {
  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val data =
          <reporter-bugs-by-this-week>
            <reporter-bugs/>
          </reporter-bugs-by-this-week>

      owner ! ReportDataResponse(reportId, data)
  }
}

object ReportersBugNumberByThisWeekActor {
  def props(owner: ActorRef) = Props(classOf[ReportersBugNumberByThisWeekActor], owner)
}

