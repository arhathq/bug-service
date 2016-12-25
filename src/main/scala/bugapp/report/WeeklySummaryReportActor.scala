package bugapp.report

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.repository.Bug

import scala.xml.Elem

/**
  * @author Alexander Kuleshov
  */
class WeeklySummaryReportActor(owner: ActorRef) extends Actor with ActorLogging {
  import WeeklySummaryReportActor._

  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]

      val bugsForCurrentWeek = bugs.filter(bug => bug.opened.isAfter(endDate.minusWeeks(1)))
      val bugsForLastWeek = bugs.filter(bug => bug.opened.isAfter(endDate.minusWeeks(2)) && bug.opened.isBefore(endDate.minusWeeks(1)))
      val bugsForLast15Week = bugs.filter(bug => bug.opened.isAfter(endDate.minusWeeks(15)))

      val data =
        <week-summary-report>
          <bugs-count>
            {tableElem(bugsForCurrentWeek, bugsForLast15Week)}
          </bugs-count>
        </week-summary-report>

      owner ! ReportDataResponse(reportId, data)
  }

  private def tableRowElem(line: String, closed: Double, open: Double, invalid: Double) = {
    <row>
      <line>{line}</line>
      <invalid>{invalid}</invalid>
      <closed>{closed}</closed>
      <open>{open}</open>
      <total>{open + closed + invalid}</total>
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

    <table>
      {tableRowElem("Mark", closedBugNumber, openedBugNumber, invalidBugNumber)}
      {tableRowElem("Average", averageClosedBugNumber, averageOpenedBugNumber, averageInvalidBugNumber)}
      {tableRowElem("Total", totalClosedBugNumber, totalOpenedBugNumber, totalInvalidBugNumber)}
    </table>
  }
}

object WeeklySummaryReportActor {
  def props(owner: ActorRef) = Props(classOf[WeeklySummaryReportActor], owner)

  def average(v1: Int, v2: Int): Double = if (v2 == 0) 0.0 else v1 / v2
}