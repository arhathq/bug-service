package bugapp.report

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

import akka.actor.{ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilderActor.{ReportDataRequest, ReportDataResponse}
import bugapp.report.model._
import bugapp.repository._

/**
  * @author Alexander Kuleshov
  */
class WeeklySummaryReportActor(owner: ActorRef) extends ReportWorker(owner) with ActorLogging {
  import WeeklySummaryReportActor._

  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]
      val startDate = endDate.minusDays(7).truncatedTo(ChronoUnit.DAYS)
      val weeks = reportParams(ReportParams.WeekPeriod).asInstanceOf[Int]

      val bugsForLastWeek = bugs.filter { bug =>
        bug.actualDate.isAfter(startDate) && bug.actualDate.isBefore(endDate)
      }
      val bugsForLast15Week = bugs.filter { bug =>
        bug.actualDate.isAfter(endDate.minusWeeks(15).truncatedTo(ChronoUnit.DAYS)) && bug.actualDate.isBefore(endDate)
      }
      log.debug(s"Bugs for last 15 weeks: ${bugsForLast15Week.size}")

      val currentWeek = Metrics.marks(endDate, 1).head

      val data =
        ReportData("week-summary-report",
          MapValue(
            productionQueueData(bugs, startDate, endDate),
            weeklyStatisticsData(bugsForLastWeek, startDate, endDate),
            bugCountData(bugsForLastWeek, bugsForLast15Week, weeks, currentWeek)
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

  private def bugCountData(bugs: Seq[Bug], totalBugs: Seq[Bug], weeks: Int, currentWeek: String): ReportField = {

    val bugsGrouped = bugs.groupBy(bug => bug.actualStatus)
    val openedBugNumber = bugsGrouped.getOrElse(Metrics.OpenStatus, Seq()).size
    val closedBugNumber = bugsGrouped.getOrElse(Metrics.FixedStatus, Seq()).size
    val invalidBugNumber = bugsGrouped.getOrElse(Metrics.InvalidStatus, Seq()).size

    val totalBugsGrouped = totalBugs.groupBy(bug => bug.actualStatus)
    val totalOpenedBugNumber = totalBugsGrouped.getOrElse(Metrics.OpenStatus, Seq()).size
    val totalClosedBugNumber = totalBugsGrouped.getOrElse(Metrics.FixedStatus, Seq()).size
    val totalInvalidBugNumber = totalBugsGrouped.getOrElse(Metrics.InvalidStatus, Seq()).size

    val averageOpenedBugNumber = average(totalOpenedBugNumber, weeks)
    val averageClosedBugNumber = average(totalClosedBugNumber, weeks)
    val averageInvalidBugNumber = average(totalInvalidBugNumber, weeks)

    val totalBugNumber = closedBugNumber + openedBugNumber + invalidBugNumber
    val totalOfTotalBugNumber = totalClosedBugNumber + totalOpenedBugNumber + totalInvalidBugNumber
    val totalAverageBugNumber = average(totalOfTotalBugNumber, weeks)



    ReportField("bugs-count",
      MapValue(
        ReportField("period", IntValue(weeks)),
        ReportField("table",
          ReportField("row",
            ListValue(
              tableRowData(currentWeek, closedBugNumber, openedBugNumber, invalidBugNumber, totalBugNumber),
              tableRowData("Average", averageClosedBugNumber, averageOpenedBugNumber, averageInvalidBugNumber, totalAverageBugNumber),
              tableRowData("Total", totalClosedBugNumber, totalOpenedBugNumber, totalInvalidBugNumber, totalOfTotalBugNumber)
            )
          )
        )
      )
    )
  }

  private def productionQueueData(bugs: Seq[Bug], startDate: OffsetDateTime, endDate: OffsetDateTime): ReportField = {
    val queueBugsCount = bugs.count(bug => bug.actualStatus == Metrics.OpenStatus)
    val highPriorityBugsCount = bugs.count { bug =>
      bug.actualStatus == Metrics.OpenStatus && (bug.priority == Metrics.P1Priority || bug.priority == Metrics.P2Priority)
    }
    val blockedBugsCount = bugs.count(bug => bug.status == "BLOCKED" && bug.actualStatus == Metrics.OpenStatus)

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
    val reopenedBugsCount = bugs.filter(bug => bug.reopenedCount > 0).foldLeft(0)((acc, bug) =>
      acc + bug.events.count {
        case BugReopenedEvent(_, _, when, _) if when.isAfter(startDate) => true
        case _ => false
      }
    )
    val movedBugsCount = bugs.filter(bug => bug.changed.isAfter(startDate) && bug.isNotResolved).foldLeft(0)((acc, bug) =>

      acc + bug.events.count {
        case BugMarkedAsProductionEvent(_, _, when, _) if when.isAfter(startDate) => true
        case BugComponentChangedEvent(_, _, when, "Dataload Failed", _) if when.isAfter(startDate) => true
        case BugComponentChangedEvent(_, _, when, "New Files Arrived", _) if when.isAfter(startDate) => true
        case BugComponentChangedEvent(_, _, when, "Data Consistency", _) if when.isAfter(startDate) => true
        case _ => false
      }

    )
    val resolvedBugsCount = bugs.filter(bug => bug.changed.isAfter(startDate)).foldLeft(0)((acc, bug) =>
      acc + bug.events.count {
        case BugResolvedEvent(_, _, when, _) if when.isAfter(startDate) => true
        case _ => false
      }
    )
    val updatedBugsCount = bugs.count(bug => bug.changed.isAfter(startDate))
    val totalBugCommentsCount = 0
//    val totalBugCommentsCount = bugs.filter(bug => bug.changed.isAfter(startDate)).foldLeft(0)((acc, bug) =>
//      acc + bug.history.getOrElse(BugHistory(bug.id, None, Seq())).items.count(item => item.when.isAfter(startDate))
//    )

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