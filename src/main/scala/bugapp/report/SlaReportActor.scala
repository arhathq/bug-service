package bugapp.report

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import akka.actor.{Actor, ActorLogging, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportData, ReportDataRequest, ReportDataResponse}
import bugapp.repository.Bug
import org.jfree.data.category.DefaultCategoryDataset

import scala.xml._

/**
  *
  */
class SlaReportActor extends Actor with ActorLogging {
  import bugapp.report.SlaReportActor._

  private val dateFormat = DateTimeFormatter.ISO_ZONED_DATE_TIME

  override def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val startDate = reportParams(ReportParams.StartDate).asInstanceOf[OffsetDateTime]
      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]
      val weekPeriod = reportParams(ReportParams.WeekPeriod).asInstanceOf[Int]
      val bugtrackerUri = reportParams(ReportParams.BugtrackerUri).asInstanceOf[String]

      val marks = Metrics.marks(endDate, weekPeriod)
      val actualBugs = bugs.filter(bugsForPeriod(_, startDate))
      val outSlaBugs = bugsOutSla(actualBugs)
      val p1OutSla = outSlaBugs.getOrElse(Metrics.P1Priority, Seq())
      val p2OutSla = outSlaBugs.getOrElse(Metrics.P2Priority, Seq())

      log.debug(s"Start date $startDate (${Metrics.weeksStrFormat.format(startDate)})")
      log.debug(s"End date $endDate (${Metrics.weeksStrFormat.format(endDate)})")
      log.debug(s"Week Period $weekPeriodElem")
      log.debug(s"Marks: $marks")
      log.debug(s"Filtered Bugs: $actualBugs")
      log.debug(s"Bugs Out SLA: $outSlaBugs")

      val data =
        <sla>
          {bugsOutSlaElem(outSlaBugs, marks, bugtrackerUri)}
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

      val reportType = reportParams(ReportParams.ReportType).asInstanceOf[String]
      context.parent ! ReportDataResponse(ReportData(reportId, reportType, data))
  }

  def bugsOutSlaElem(bugs: Map[String, Seq[Bug]], marks: Seq[String], bugtrackerUri: String): Elem = {
    val p1 = bugs.getOrElse(Metrics.P1Priority, Seq())
    val p2 = bugs.getOrElse(Metrics.P2Priority, Seq())
    val p1p2 = p1 ++ p2

    <out-sla-bugs>
      <table>
        {outSlaTableElem(Metrics.P1Priority, p1, bugtrackerUri)}
        {outSlaTableElem(Metrics.P2Priority, p2, bugtrackerUri)}
        {outSlaTableElem("Grand Total", p1p2, bugtrackerUri)}
      </table>
      <list>
        {bugListElem(p1, bugtrackerUri)}
        {bugListElem(p2, bugtrackerUri)}
      </list>
      <image>
        <content-type>image/jpeg</content-type>
        <content-value>{outSlaChart(marks, p1p2)}</content-value>
      </image>
    </out-sla-bugs>
  }

  def outSlaTableElem(priority: String, bugs: Seq[Bug], bugtrackerUri: String): Elem = {
    val grouped = bugs.groupBy(_.stats.status)
    val opened = grouped.getOrElse(Metrics.OpenStatus, Seq()).length
    val fixed = grouped.getOrElse(Metrics.FixedStatus, Seq()).length
    val invalid = grouped.getOrElse(Metrics.InvalidStatus, Seq()).length
    val ids = bugs.collect({case b: Bug => b.id}).mkString(",")
    <record>
      <priority>{priority}</priority>
      <fixed>{fixed}</fixed>
      <invalid>{invalid}</invalid>
      <opened>{opened}</opened>
      <total>{fixed + opened + invalid}</total>
      <link>{s"$bugtrackerUri/buglist.cgi?bug_id=$ids"}</link>
    </record>
  }

  def bugListElem(bugs: Seq[Bug], bugtrackerUri: String): Seq[Elem] = {
    bugs.map {bug =>
      <bug>
        <id>{bug.id}</id>
        <priority>{bug.priority}</priority>
        <opened>{dateFormat.format(bug.opened)}</opened>
        <resolved>{
           bug.stats.resolvedTime match {
             case Some(time) => dateFormat.format(time)
             case None => ""
            }
          }</resolved>
        <daysOpen>{bug.stats.daysOpen}</daysOpen>
        <reopenedCount>{bug.stats.reopenCount}</reopenedCount>
        <summary>{PCData(bug.summary)}</summary>
        <link>{s"$bugtrackerUri/show_bug.cgi?id=${bug.id}"}</link>
      </bug>
    }
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

  def outSlaChartData(priority: String, marks:Seq[String], bugs: Seq[Bug]): Seq[(Int, String, String)] = {
    val grouped = bugs.groupBy(bug => bug.stats.openMonth)
    marks.map { mark =>
      grouped.get(mark) match {
        case Some(v) => (v.length, priority, mark)
        case None => (0, priority, mark)
      }
    }
  }

  def outSlaChart(marks:Seq[String], bugs: Seq[Bug]): String = {
    val dataSet = new DefaultCategoryDataset()
    outSlaChartData(Metrics.P1Priority, marks, bugs.filter(_.priority == Metrics.P1Priority)).foreach { v => dataSet.addValue(v._1, v._2, v._3) }
    outSlaChartData(Metrics.P2Priority, marks, bugs.filter(_.priority == Metrics.P2Priority)).foreach { v => dataSet.addValue(v._1, v._2, v._3) }
    ChartGenerator.generateBase64OutSlaBugs(dataSet)
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
  def props() = Props(classOf[SlaReportActor])

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

  val slaPercentage: (Int, Int) => Double = (count, totalCount) => if (totalCount < 1) 100.0 else (count * 100 / totalCount).toDouble

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