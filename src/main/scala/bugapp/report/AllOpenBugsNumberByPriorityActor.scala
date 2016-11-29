package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}

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
         owner ! ReportDataResponse(reportId, data)
  }
}

object AllOpenBugsNumberByPriorityActor {
  def props(owner: ActorRef) = Props(classOf[AllOpenBugsNumberByPriorityActor], owner)
}