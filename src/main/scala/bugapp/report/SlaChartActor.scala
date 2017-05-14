package bugapp.report

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

import akka.actor.{ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilderActor.{ReportDataRequest, ReportDataResponse}
import bugapp.report.model._
import bugapp.repository.Bug
import org.jfree.data.category.DefaultCategoryDataset

/**
  * @author Alexander Kuleshov
  */
class SlaChartActor(owner: ActorRef, renderChart: Boolean) extends ReportWorker(owner) with ActorLogging {
  import SlaChartActor._

  override def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>

      val weekPeriod = reportParams(ReportParams.WeekPeriod).asInstanceOf[Int]
      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]
      val startDate = endDate.minusWeeks(weekPeriod - 1).truncatedTo(ChronoUnit.DAYS)
      val bugtrackerUri = reportParams(ReportParams.BugtrackerUri).asInstanceOf[String]

      val marks = Metrics.marks(endDate, weekPeriod)
      val actualBugs = bugs.filter(SlaReportActor.bugsForPeriod(_, startDate))

      val chart =
        if (renderChart)
          slaAchievementTrendChart(marks, actualBugs)
        else
          slaAchievementTrendChartData(marks, actualBugs)

      val data = model.ReportData("sla-chart", MapValue(chart))

      owner ! ReportDataResponse(reportId, data)
  }

}
object SlaChartActor {
  def props(owner: ActorRef, renderChart: Boolean = true) = Props(classOf[SlaChartActor], owner, renderChart)

  def slaAchievementTrendChartData(priority: String, marks:Seq[String], bugs: Seq[Bug]): Seq[(Double, String, String)] = {
    val grouped = bugs.groupBy(bug => bug.openMonth)
    marks.map { mark =>
      grouped.get(mark) match {
        case Some(v) => (SlaReportActor.slaPercentage(v.count(_.passSla), v.length), priority, mark)
        case None => (100.0, priority, mark)
      }
    }
  }

  def slaAchievementTrendChart(marks:Seq[String], bugs: Seq[Bug]): ReportField = {
    val dataSet = new DefaultCategoryDataset()
    slaAchievementTrendChartData(Metrics.P1Priority, marks, bugs.filter(_.priority == Metrics.P1Priority)).foreach {v => dataSet.addValue(v._1, v._2, v._3)}
    slaAchievementTrendChartData(Metrics.P2Priority, marks, bugs.filter(_.priority == Metrics.P2Priority)).foreach {v => dataSet.addValue(v._1, v._2, v._3)}

    ReportField("image",
      MapValue(
        ReportField("content-type", StringValue("image/jpeg")),
        ReportField("content-value", StringValue(ChartGenerator.generateBase64SlaAchievementTrend(dataSet)))
      )
    )
  }

  def slaAchievementTrendChartData(marks:Seq[String], bugs: Seq[Bug]): ReportField = {
    ReportField("datasets",
      MapValue(
        ReportField("labels",
          ListValue(
            marks.map(mark => StringValue(mark)): _*
          )
        ),
        ReportField("dataset",
          ListValue(
            slaAchievementTrendDataSet(Metrics.P1Priority, marks, bugs),
            slaAchievementTrendDataSet(Metrics.P2Priority, marks, bugs)
          )
        )
      )
    )
  }

  def slaAchievementTrendDataSet(priority: String, marks:Seq[String], bugs: Seq[Bug]): ReportValue = {
    val data = slaAchievementTrendChartData(priority, marks, bugs.filter(_.priority == priority))
    MapValue(
      ReportField("name", StringValue(priority)),
      ReportField("values",
        ListValue(
          data.indices.map { idx =>
            MapValue(
              ReportField("x", IntValue(idx)),
              ReportField("y", BigDecimalValue(BigDecimal(data(idx)._1)))
            )
          }: _*
        )
      )
    )
  }
}