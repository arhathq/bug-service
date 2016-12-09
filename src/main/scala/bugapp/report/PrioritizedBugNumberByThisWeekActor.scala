package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}

/**
  * @author Alexander Kuleshov
  */
class PrioritizedBugNumberByThisWeekActor(owner: ActorRef) extends Actor with ActorLogging {
  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val data =
        <priority-bugs-by-this-week>
          <priority-bugs/>
        </priority-bugs-by-this-week>

      owner ! ReportDataResponse(reportId, data)
  }
}

object PrioritizedBugNumberByThisWeekActor {
  def props(owner: ActorRef) = Props(classOf[PrioritizedBugNumberByThisWeekActor], owner)
}