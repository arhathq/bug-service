package bugapp.report

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportActor.formatNumber
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.repository.Bug

import scala.xml.Elem

/**
  * @author Alexander Kuleshov
  */
class PrioritizedBugNumberByThisWeekActor(owner: ActorRef) extends Actor with ActorLogging {
  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]
      val startDate = endDate.minusWeeks(1)

      val weeklyBugs = bugs.filter(bug => bug.opened.isAfter(startDate))
      val bugsByPriority = weeklyBugs.groupBy(bug => bug.priority)

      val data =
        <priority-bugs-by-this-week>
          {bugsByPriority.toSeq.sortWith(_._1 < _._1).map {case (priority, prioritizedBugs) => priorityBugsNumElem(priority, prioritizedBugs)}}
          {priorityBugsNumElem("Grand Total", weeklyBugs)}
        </priority-bugs-by-this-week>

      owner ! ReportDataResponse(reportId, data)
  }

  def priorityBugsNumElem(priority: String, bugs: Seq[Bug]): Elem = {
    val closed = bugs.count(bug => bug.stats.status == Metrics.FixedStatus)
    val invalid = bugs.count(bug => bug.stats.status == Metrics.InvalidStatus)
    val opened = bugs.count(bug => bug.stats.status == Metrics.OpenStatus)

    <priority-bugs>
      <priority>{priority}</priority>
      <closed>{formatNumber(closed)}</closed>
      <invalid>{formatNumber(invalid)}</invalid>
      <opened>{formatNumber(opened)}</opened>
      <total>{formatNumber(closed + invalid + opened)}</total>
    </priority-bugs>
  }
}

object PrioritizedBugNumberByThisWeekActor {
  def props(owner: ActorRef) = Props(classOf[PrioritizedBugNumberByThisWeekActor], owner)
}