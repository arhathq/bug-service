package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.report.ReportDataBuilder.{ReportData, ReportDataRequest, ReportDataResponse}

/**
  * @author Alexander Kuleshov
  */
class AllOpenBugsNumberByPriorityActor(owner: ActorRef) extends Actor with ActorLogging {

  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
        val data =
          <all-open-bugs>
            <prioritized-bugs/>
          </all-open-bugs>
         owner ! ReportDataResponse(ReportData(reportId, reportParams(ReportParams.ReportType).asInstanceOf[String], data))
  }
}

object AllOpenBugsNumberByPriorityActor {
  def props(owner: ActorRef) = Props(classOf[AllOpenBugsNumberByPriorityActor], owner)
}