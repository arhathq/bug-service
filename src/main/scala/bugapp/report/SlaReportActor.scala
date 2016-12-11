package bugapp.report

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.repository.Bug
import org.jfree.data.category.DefaultCategoryDataset

import scala.xml._

/**
  *
  */
class SlaReportActor(owner: ActorRef) extends Actor with ActorLogging {
  import bugapp.report.SlaReportActor._

  override def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val startDate = reportParams(ReportParams.StartDate).asInstanceOf[OffsetDateTime]
      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]
      val weekPeriod = reportParams(ReportParams.WeekPeriod).asInstanceOf[Int]
      val bugtrackerUri = reportParams(ReportParams.BugtrackerUri).asInstanceOf[String]
      val reportType = reportParams(ReportParams.ReportType).asInstanceOf[String]

      val marks = Metrics.marks(endDate, weekPeriod)
      val actualBugs = bugs.filter(bugsForPeriod(_, startDate))
      val outSlaBugs = bugsOutSla(actualBugs)
      val p1OutSla = outSlaBugs.getOrElse(Metrics.P1Priority, Seq())
      val p2OutSla = outSlaBugs.getOrElse(Metrics.P2Priority, Seq())

      val data =
        <sla>
          <p1-sla>
            {sla(Metrics.P1Priority, marks, p1OutSla, actualBugs.filter(_.priority == Metrics.P1Priority))}
          </p1-sla>
          <p2-sla>
            {sla(Metrics.P2Priority, marks, p2OutSla, actualBugs.filter(_.priority == Metrics.P2Priority))}
          </p2-sla>
          <sla-chart>
            <image>
              <content-type>image/jpeg</content-type>
              <content-value>{slaAchievementTrendChart(marks, actualBugs)}</content-value>
            </image>
          </sla-chart>
        </sla>

      owner ! ReportDataResponse(reportId, data)
  }

  def slaAchievementTrendChartData(priority: String, marks:Seq[String], bugs: Seq[Bug]): Seq[(Double, String, String)] = {
    val grouped = bugs.groupBy(bug => bug.stats.openMonth)
    marks.map { mark =>
      grouped.get(mark) match {
        case Some(v) => (slaPercentage(v.count(_.stats.passSla), v.length), priority, mark)
        case None => (100.0, priority, mark)
      }
    }
  }

  def slaAchievementTrendChart(marks:Seq[String], bugs: Seq[Bug]): String = {
    val dataSet = new DefaultCategoryDataset()
    slaAchievementTrendChartData(Metrics.P1Priority, marks, bugs.filter(_.priority == Metrics.P1Priority)).foreach {v => dataSet.addValue(v._1, v._2, v._3)}
    slaAchievementTrendChartData(Metrics.P2Priority, marks, bugs.filter(_.priority == Metrics.P2Priority)).foreach {v => dataSet.addValue(v._1, v._2, v._3)}
    ChartGenerator.generateBase64SlaAchievementTrend(dataSet)
  }

  def sla(priority: String, marks:Seq[String], bugsOutSla: Seq[Bug], allBugs: Seq[Bug]): Elem = {
    val outSlaGrouped = bugsOutSla.groupBy(bug => bug.stats.openMonth)
    val allGrouped = allBugs.groupBy(bug => bug.stats.openMonth)
    val res = marks.map(mark => outSlaGrouped.get(mark) match {
      case Some(v) =>
        weekPeriodElem(priority, mark, v.length, allGrouped.getOrElse(mark, Seq()).length)
      case None =>
        weekPeriodElem(priority, mark, 0, allGrouped.getOrElse(mark, Seq()).length)

    })
    val total =
      weekPeriodElem(priority, "Grand Total", outSlaGrouped.map(v => v._2.length).sum, allBugs.length)
    Elem.apply(null, s"sla-achievement", Null, TopScope, true, Group(res), total)
  }

}

object SlaReportActor {
  def props(owner: ActorRef) = Props(classOf[SlaReportActor], owner)

  val bugsForPeriod: (Bug, OffsetDateTime) => Boolean = (bug, startDate) => {
    bug.priority match {
      case Metrics.P1Priority if bug.opened.isAfter(startDate) => true
      case Metrics.P2Priority if bug.opened.isAfter(startDate) => true
      case _ => false
    }
  }

  val slaBugs: (Seq[Bug]) => Map[String, Seq[Bug]] = bugs => {
    bugs.groupBy(bug => bug.stats.resolvedPeriod)
  }

  val bugsOutSla: (Seq[Bug]) => Map[String, Seq[Bug]] = bugs => {
    bugs.groupBy(bug => bug.stats match {
      case stats if !stats.passSla && bug.priority == Metrics.P1Priority => Metrics.P1Priority
      case stats if !stats.passSla && bug.priority == Metrics.P2Priority => Metrics.P2Priority
      case _ => "sla"
    })
  }

  val slaPercentage: (Int, Int) => Double = (count, totalCount) => if (totalCount < 1) 100.0 else count * 100.0 / totalCount

  val weekPeriodElem: (String, String, Int, Int) => Elem = (priority, mark, outSlaCount, totalCount) => {
    <week-period>
      <priority>{priority}</priority>
      <week>{mark}</week>
      <slaPercentage>{slaPercentage(totalCount - outSlaCount, totalCount)}</slaPercentage>
      <slaCount>{totalCount - outSlaCount}</slaCount>
      <outSlaCount>{outSlaCount}</outSlaCount>
      <totalCount>{totalCount}</totalCount>
    </week-period>
  }
}