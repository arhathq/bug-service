package bugapp.report

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.report.model._
import bugapp.repository.{Bug, BugHistory}

/**
  * @author Alexander Kuleshov
  */
class WeeklySummaryReportActor(owner: ActorRef) extends Actor with ActorLogging {
  import WeeklySummaryReportActor._

  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]
      val startDate = endDate.minusDays(7)
      val weeks = reportParams(ReportParams.WeekPeriod).asInstanceOf[Int]

      val bugsForCurrentWeek = bugs.filter(bug => bug.opened.isAfter(startDate))
      val bugsForLastWeek = bugs.filter(bug => bug.opened.isAfter(endDate.minusWeeks(2)) && bug.opened.isBefore(endDate.minusWeeks(1)))
      val bugsForLast15Week = bugs.filter(bug => bug.opened.isAfter(endDate.minusWeeks(15)))

      val data =
        ReportData("week-summary-report",
          MapValue(
            productionQueueData(bugs, startDate, endDate),
            weeklyStatisticsData(bugs, startDate, endDate),
            bugCountData(bugsForCurrentWeek, bugsForLast15Week, weeks)
          )
        )

      owner ! ReportDataResponse(reportId, data)
  }

  private def tableRowData(line: String, closed: Double, open: Double, invalid: Double, total: Double): MapValue = {
    MapValue(
      ReportField("line", StringValue(line)),
      ReportField("invalid", BigDecimalValue(invalid)),
      ReportField("closed", BigDecimalValue(closed)),
      ReportField("open", BigDecimalValue(open)),
      ReportField("total", BigDecimalValue(total))
    )
  }

  private def bugCountData(bugs: Seq[Bug], totalBugs: Seq[Bug], weeks: Int): ReportField = {

    val bugsGrouped = bugs.groupBy(bug => bug.stats.status)
    val openedBugNumber = bugsGrouped.getOrElse(Metrics.OpenStatus, Seq()).size
    val closedBugNumber = bugsGrouped.getOrElse(Metrics.FixedStatus, Seq()).size
    val invalidBugNumber = bugsGrouped.getOrElse(Metrics.InvalidStatus, Seq()).size

    val totalBugsGrouped = totalBugs.groupBy(bug => bug.stats.status)
    val totalOpenedBugNumber = totalBugsGrouped.getOrElse(Metrics.OpenStatus, Seq()).size
    val totalClosedBugNumber = totalBugsGrouped.getOrElse(Metrics.FixedStatus, Seq()).size
    val totalInvalidBugNumber = totalBugsGrouped.getOrElse(Metrics.InvalidStatus, Seq()).size

    val averageOpenedBugNumber = average(totalOpenedBugNumber, openedBugNumber)
    val averageClosedBugNumber = average(totalClosedBugNumber, closedBugNumber)
    val averageInvalidBugNumber = average(totalInvalidBugNumber, invalidBugNumber)

    val totalBugNumber = closedBugNumber + openedBugNumber + invalidBugNumber
    val totalOfTotalBugNumber = totalClosedBugNumber + totalOpenedBugNumber + totalInvalidBugNumber
    val totalAverageBugNumber = average(totalOfTotalBugNumber, totalBugNumber)

    ReportField("bugs-count",
      MapValue(
        ReportField("period", IntValue(weeks)),
        ReportField("table",
          ReportField("row",
            ListValue(
              tableRowData("Mark", closedBugNumber, openedBugNumber, invalidBugNumber, totalBugNumber),
              tableRowData("Average", averageClosedBugNumber, averageOpenedBugNumber, averageInvalidBugNumber, totalAverageBugNumber),
              tableRowData("Total", totalClosedBugNumber, totalOpenedBugNumber, totalInvalidBugNumber, totalOfTotalBugNumber)
            )
          )
        )
      )
    )
  }

  private def productionQueueData(bugs: Seq[Bug], startDate: OffsetDateTime, endDate: OffsetDateTime): ReportField = {
    val queueBugsCount = bugs.count(bug => bug.stats.status == Metrics.OpenStatus)
    val highPriorityBugsCount = bugs.count { bug =>
      bug.stats.status == Metrics.OpenStatus && (bug.priority == Metrics.P1Priority || bug.priority == Metrics.P2Priority)
    }
    val blockedBugsCount = bugs.count(bug => bug.status == "BLOCKED" && bug.stats.status == Metrics.OpenStatus)

    ReportField("production-queue",
      MapValue(
        ReportField("state", StringValue("changed")),
        ReportField("from", IntValue(0)),
        ReportField("to", IntValue(queueBugsCount)),
        ReportField("high-priotity-bugs", IntValue(highPriorityBugsCount)),
        ReportField("blocked-bugs", IntValue(blockedBugsCount))
      )
    )
  }

  private def weeklyStatisticsData(bugs: Seq[Bug], startDate: OffsetDateTime, endDate: OffsetDateTime): ReportField = {
    val newBugsCount = bugs.count(bug => bug.opened.isAfter(startDate) && bug.opened.isBefore(endDate))
    val reopenedBugsCount = bugs.count(bug => bug.status == "REOPENED" && bug.changed.isAfter(startDate))
    val movedBugsCount = bugs.count(bug => bug.priority != "NP" && bug.changed.isAfter(startDate))
    val resolvedBugsCount = bugs.count(bug => bug.stats.status == Metrics.FixedStatus && bug.changed.isAfter(startDate))
    val updatedBugsCount = bugs.count(bug => bug.changed.isAfter(startDate))
    val totalBugCommentsCount = bugs.filter(bug => bug.changed.isAfter(startDate)).foldLeft(0)((acc, bug) =>
      acc + bug.history.getOrElse(BugHistory(bug.id, None, Seq())).items.count(item => item.when.isAfter(startDate))
    )

    ReportField("statistics",
      MapValue(
        ReportField("new", IntValue(newBugsCount)),
        ReportField("reopened", IntValue(reopenedBugsCount)),
        ReportField("moved", IntValue(movedBugsCount)),
        ReportField("resolved", IntValue(resolvedBugsCount)),
        ReportField("bugs-updated", IntValue(updatedBugsCount)),
        ReportField("total-comments", IntValue(totalBugCommentsCount))
      )
    )
  }

}

object WeeklySummaryReportActor {
  def props(owner: ActorRef) = Props(classOf[WeeklySummaryReportActor], owner)

  def average(v1: Int, v2: Int): Double = if (v2 == 0) 0.0 else v1.toDouble / v2
}