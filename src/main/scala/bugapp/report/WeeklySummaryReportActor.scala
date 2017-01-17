package bugapp.report

import java.time.{DayOfWeek, OffsetDateTime}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportActor._
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.repository.{Bug, BugHistory}

import scala.xml.{Elem, Group, Text}

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
        <week-summary-report>
          {productionQueueElem(bugs, startDate, endDate)}
          {weeklyStatisticsElem(bugs, startDate, endDate)}
          <bugs-count>
            <period>{weeks}</period>
            {tableElem(bugsForCurrentWeek, bugsForLast15Week)}
          </bugs-count>
        </week-summary-report>

      owner ! ReportDataResponse(reportId, data)
  }

  private def tableRowElem(line: String, closed: Double, open: Double, invalid: Double, total: Double) = {
    <row>
      <line>{line}</line>
      <invalid>{invalid}</invalid>
      <closed>{closed}</closed>
      <open>{open}</open>
      <total>{total}</total>
    </row>
  }

  private def tableElem(bugs: Seq[Bug], totalBugs: Seq[Bug]): Elem = {

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

    <table>
      {tableRowElem("Mark", closedBugNumber, openedBugNumber, invalidBugNumber, totalBugNumber)}
      {tableRowElem("Average", averageClosedBugNumber, averageOpenedBugNumber, averageInvalidBugNumber, totalAverageBugNumber)}
      {tableRowElem("Total", totalClosedBugNumber, totalOpenedBugNumber, totalInvalidBugNumber, totalOfTotalBugNumber)}
    </table>
  }

  private def productionQueueElem(bugs: Seq[Bug], startDate: OffsetDateTime, endDate: OffsetDateTime): Elem = {
  val queueBugsCount = bugs.count(bug => bug.stats.status == Metrics.OpenStatus)
  val highPriorityBugsCount = bugs.count { bug =>
    bug.stats.status == Metrics.OpenStatus && (bug.priority == Metrics.P1Priority || bug.priority == Metrics.P2Priority)
  }
  val blockedBugsCount = bugs.count(bug => bug.status == "BLOCKED" && bug.stats.status == Metrics.OpenStatus)
    <production-queue>
      <state>changed</state>
      <from>0</from>
      <to>{queueBugsCount}</to>
      <high-priotity-bugs>{highPriorityBugsCount}</high-priotity-bugs>
      <blocked-bugs>{blockedBugsCount}</blocked-bugs>
    </production-queue>
  }

  private def weeklyStatisticsElem(bugs: Seq[Bug], startDate: OffsetDateTime, endDate: OffsetDateTime): Elem = {
    val newBugsCount = bugs.count(bug => bug.opened.isAfter(startDate) && bug.opened.isBefore(endDate))
    val reopenedBugsCount = bugs.count(bug => bug.status == "REOPENED" && bug.changed.isAfter(startDate))
    val movedBugsCount = bugs.count(bug => bug.priority != "NP" && bug.changed.isAfter(startDate))
    val resolvedBugsCount = bugs.count(bug => bug.stats.status == Metrics.FixedStatus && bug.changed.isAfter(startDate))
    val updatedBugsCount = bugs.count(bug => bug.changed.isAfter(startDate))
    val totalBugCommentsCount = bugs.filter(bug => bug.changed.isAfter(startDate)).foldLeft(0)((acc, bug) =>
      acc + bug.history.getOrElse(BugHistory(bug.id, None, Seq())).items.count(item => item.when.isAfter(startDate))
    )
    <statistics>
      <new>{newBugsCount}</new>
      <reopened>{reopenedBugsCount}</reopened>
      <moved>{movedBugsCount}</moved>
      <resolved>{resolvedBugsCount}</resolved>
      <bugs-updated>{updatedBugsCount}</bugs-updated>
      <total-comments>{totalBugCommentsCount}</total-comments>
    </statistics>
  }

}

object WeeklySummaryReportActor {
  def props(owner: ActorRef) = Props(classOf[WeeklySummaryReportActor], owner)

  def average(v1: Int, v2: Int): Double = if (v2 == 0) 0.0 else v1.toDouble / v2
}