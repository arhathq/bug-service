package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.report.model.{IntValue, MapValue, ReportField, StringValue}


/**
  * @author Alexander Kuleshov
  */
class TopAssigneesActor(owner: ActorRef) extends Actor with ActorLogging {
  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val bugsByAssignee = bugs.filter(bug => bug.stats.status == Metrics.OpenStatus).groupBy(bug => bug.assignee)

      val topBugAssignees = bugsByAssignee.map(tuple => (tuple._1, tuple._2.length)).toSeq.sortWith(_._2 > _._2).take(15)

      val data =
        model.ReportData("top-asignees",
          MapValue(
            topBugAssignees.map(topBugsAssigneeData): _*
          )
        )

      owner ! ReportDataResponse(reportId, data)
  }

  def topBugsAssigneeData(assignee: (String, Int)): ReportField = {
    ReportField("asignee",
      MapValue(
        ReportField("name", StringValue(assignee._1)),
        ReportField("count", IntValue(assignee._2))
      )
    )
  }
}

object TopAssigneesActor {
  def props(owner: ActorRef) = Props(classOf[TopAssigneesActor], owner)
}