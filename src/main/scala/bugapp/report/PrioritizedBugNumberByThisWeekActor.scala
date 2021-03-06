package bugapp.report

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

import akka.actor.{ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportActor.formatNumber
import bugapp.report.ReportDataBuilderActor.{ReportDataRequest, ReportDataResponse}
import bugapp.report.model.{ListValue, MapValue, ReportField, StringValue}
import bugapp.repository.Bug


/**
  * @author Alexander Kuleshov
  */
class PrioritizedBugNumberByThisWeekActor(owner: ActorRef) extends ReportWorker(owner) with ActorLogging {
  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]
      val startDate = endDate.minusWeeks(1).truncatedTo(ChronoUnit.DAYS)

      val weeklyBugs = bugs.filter {bug => bug.actualDate.isAfter(startDate) && bug.actualDate.isBefore(endDate)}
      log.debug(s"Filtered bugs number: ${weeklyBugs.size}")
      val bugsByPriority = weeklyBugs.groupBy(bug => bug.priority)

      val priorityBugsNumValue =
        bugsByPriority.toSeq.sortWith(_._1 < _._1).
          map {case (priority, prioritizedBugs) => priorityBugsNumData(priority, prioritizedBugs)} :+
          priorityBugsNumData("Grand Total", weeklyBugs)

      val data = model.ReportData("priority-bugs-by-this-week",
        MapValue(ReportField("priority-bugs", ListValue(priorityBugsNumValue: _*)))
      )

      owner ! ReportDataResponse(reportId, data)
  }

  def priorityBugsNumData(priority: String, bugs: Seq[Bug]): MapValue = {
    val closed = bugs.count(bug => bug.actualStatus == Metrics.FixedStatus)
    val invalid = bugs.count(bug => bug.actualStatus == Metrics.InvalidStatus)
    val opened = bugs.count(bug => bug.actualStatus == Metrics.OpenStatus)

    MapValue(
      ReportField("priority", StringValue(priority)),
      ReportField("closed", StringValue(formatNumber(closed))),
      ReportField("invalid", StringValue(formatNumber(invalid))),
      ReportField("opened", StringValue(formatNumber(opened))),
      ReportField("total", StringValue(formatNumber(closed + invalid + opened)))
    )
  }
}

object PrioritizedBugNumberByThisWeekActor {
  def props(owner: ActorRef) = Props(classOf[PrioritizedBugNumberByThisWeekActor], owner)
}