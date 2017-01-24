package bugapp.report

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportActor.dateTimeFormat
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.report.model._
import bugapp.repository.Bug
import org.jfree.data.category.DefaultCategoryDataset


/**
  *
  */
class BugsOutSlaActor(owner: ActorRef) extends Actor with ActorLogging {
  import bugapp.report.SlaReportActor._

  override def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val weekPeriod = reportParams(ReportParams.WeekPeriod).asInstanceOf[Int]
      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]
      val startDate = endDate.minusWeeks(weekPeriod - 1).truncatedTo(ChronoUnit.DAYS)
      val bugtrackerUri = reportParams(ReportParams.BugtrackerUri).asInstanceOf[String]
      val reportType = reportParams(ReportParams.ReportType).asInstanceOf[String]

      val marks = Metrics.marks(endDate, weekPeriod)
      val actualBugs = bugs.filter(bugsForPeriod(_, startDate))
      val outSlaBugs = bugsOutSla(actualBugs)

      val data = bugsOutSlaData(outSlaBugs, marks, bugtrackerUri)

      owner ! ReportDataResponse(reportId, data)
  }

  def bugsOutSlaData(bugs: Map[String, Seq[Bug]], marks: Seq[String], bugtrackerUri: String): ReportData = {
    val p1 = bugs.getOrElse(Metrics.P1Priority, Seq())
    val p2 = bugs.getOrElse(Metrics.P2Priority, Seq())
    val p1p2 = p1 ++ p2

    <out-sla-bugs>
      <week-period>{marks.length}</week-period>
      <table>
        {outSlaTableData(Metrics.P1Priority, p1, bugtrackerUri)}
        {outSlaTableData(Metrics.P2Priority, p2, bugtrackerUri)}
        {outSlaTableData("Grand Total", p1p2, bugtrackerUri)}
      </table>
      <list>
        {bugListData(p1, bugtrackerUri)}
        {bugListData(p2, bugtrackerUri)}
      </list>
      <image>
        <content-type>image/jpeg</content-type>
        <content-value>{outSlaChart(marks, p1p2)}</content-value>
      </image>
    </out-sla-bugs>

    model.ReportData("out-sla-bugs",
      MapValue(
        ReportField("week-period", IntValue(marks.length)),
        ReportField("table",
          MapValue(
            outSlaTableData(Metrics.P1Priority, p1, bugtrackerUri),
            outSlaTableData(Metrics.P2Priority, p2, bugtrackerUri),
            outSlaTableData("Grand Total", p1p2, bugtrackerUri)
          )
        ),
        ReportField("list",
          MapValue(
            bugListData(p1, bugtrackerUri) ++ bugListData(p2, bugtrackerUri) : _*
          )
        ),
        outSlaChart(marks, p1p2)
      )
    )
  }

  def bugListData(bugs: Seq[Bug], bugtrackerUri: String): Seq[ReportField] = {
    bugs.map {bug =>
      ReportField("bug",
        MapValue(
          ReportField("id", IntValue(bug.id)),
          ReportField("priority", StringValue(bug.priority)),
          ReportField("opened", StringValue(dateTimeFormat.format(bug.opened))),
          ReportField("resolved",
            StringValue(
              bug.stats.resolvedTime match {
                case Some(time) => dateTimeFormat.format(time)
                case None => ""
              }
            )
          ),
          ReportField("daysOpen", IntValue(bug.stats.daysOpen)),
          ReportField("reopenedCount", IntValue(bug.stats.reopenCount)),
          ReportField("summary", StringValue(bug.summary)),
          ReportField("link", StringValue(s"$bugtrackerUri/show_bug.cgi?id=${bug.id}"))
        )
      )
    }
  }

  def outSlaTableData(priority: String, bugs: Seq[Bug], bugtrackerUri: String): ReportField = {
    val grouped = bugs.groupBy(_.stats.status)
    val opened = grouped.getOrElse(Metrics.OpenStatus, Seq()).length
    val fixed = grouped.getOrElse(Metrics.FixedStatus, Seq()).length
    val invalid = grouped.getOrElse(Metrics.InvalidStatus, Seq()).length
    val ids = bugs.collect({case b: Bug => b.id}).mkString(",")

    ReportField("record",
      MapValue(
        ReportField("priority", StringValue(priority)),
        ReportField("fixed", IntValue(fixed)),
        ReportField("invalid", IntValue(invalid)),
        ReportField("opened", IntValue(opened)),
        ReportField("total", IntValue(fixed + opened + invalid)),
        ReportField("link", StringValue(s"$bugtrackerUri/buglist.cgi?bug_id=$ids"))
      )
    )
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

  def outSlaChart(marks:Seq[String], bugs: Seq[Bug]): ReportField = {
    val dataSet = new DefaultCategoryDataset()
    outSlaChartData(Metrics.P1Priority, marks, bugs.filter(_.priority == Metrics.P1Priority)).foreach { v => dataSet.addValue(v._1, v._2, v._3) }
    outSlaChartData(Metrics.P2Priority, marks, bugs.filter(_.priority == Metrics.P2Priority)).foreach { v => dataSet.addValue(v._1, v._2, v._3) }

    ReportField("image",
      MapValue(
        ReportField("content-type", StringValue("image/jpeg")),
        ReportField("content-value", StringValue(ChartGenerator.generateBase64OutSlaBugs(dataSet)))
      )
    )

  }


}

object BugsOutSlaActor {
  def props(owner: ActorRef) = Props(classOf[BugsOutSlaActor], owner)
}