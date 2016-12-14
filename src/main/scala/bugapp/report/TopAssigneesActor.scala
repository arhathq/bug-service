package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}

import scala.xml.Elem

/**
  * @author Alexander Kuleshov
  */
class TopAssigneesActor(owner: ActorRef) extends Actor with ActorLogging {
  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val bugsByAssignee = bugs.filter(bug => bug.stats.status == Metrics.OpenStatus).groupBy(bug => bug.assignee)

      val topBugAssignees = bugsByAssignee.map(tuple => (tuple._1, tuple._2.length)).toSeq.sortWith(_._2 > _._2)

      val data =
        <top-asignees>
          {topBugAssignees.map(topBugsAssigneeElem)}
        </top-asignees>

      owner ! ReportDataResponse(reportId, data)
  }

  def topBugsAssigneeElem(assignee: (String, Int)): Elem = {
      <asignee>
        <name>{assignee._1}</name>
        <count>{assignee._2}</count>
      </asignee>
  }
}

object TopAssigneesActor {
  def props(owner: ActorRef) = Props(classOf[TopAssigneesActor], owner)
}