package bugapp.report

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorLogging, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportData, ReportDataRequest, ReportDataResponse}
import bugapp.repository.Bug
import org.jfree.data.category.DefaultCategoryDataset

import scala.xml.{Elem, PCData}

/**
  *
  */
class BugsOutSlaActor extends Actor with ActorLogging {
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

      val data = bugsOutSlaElem(outSlaBugs, marks, bugtrackerUri)

      context.parent ! ReportDataResponse(ReportData(reportId, reportType, data))
  }

  def bugsOutSlaElem(bugs: Map[String, Seq[Bug]], marks: Seq[String], bugtrackerUri: String): Elem = {
    val p1 = bugs.getOrElse(Metrics.P1Priority, Seq())
    val p2 = bugs.getOrElse(Metrics.P2Priority, Seq())
    val p1p2 = p1 ++ p2

    <out-sla-bugs>
      <week-period>{marks.length}</week-period>
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

}

object BugsOutSlaActor {
  def props() = Props(classOf[BugsOutSlaActor])
}