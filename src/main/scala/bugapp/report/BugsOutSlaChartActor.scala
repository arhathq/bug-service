package bugapp.report

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

import akka.actor.{ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.report.SlaReportActor.{bugsForPeriod, bugsOutSla}
import bugapp.report.model._
import bugapp.repository.Bug
import org.jfree.data.category.DefaultCategoryDataset

/**
  * Created by Oleksandr_Kulieshov on 3/20/2017.
  */
class BugsOutSlaChartActor(owner: ActorRef, renderChart: Boolean) extends ReportWorker(owner) with ActorLogging {
  import BugsOutSlaChartActor._

  override def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val weekPeriod = reportParams(ReportParams.WeekPeriod).asInstanceOf[Int]
      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]
      val startDate = endDate.minusWeeks(weekPeriod - 1).truncatedTo(ChronoUnit.DAYS)
      val bugtrackerUri = reportParams(ReportParams.BugtrackerUri).asInstanceOf[String]

      val marks = Metrics.marks(endDate, weekPeriod)
      val actualBugs = bugs.filter(bugsForPeriod(_, startDate))
      val outSlaBugs = bugsOutSla(actualBugs)

      val chart =
        if (renderChart)
          bugsOutSlaChart(outSlaBugs, marks)
        else
          bugsOutSlaData(outSlaBugs, marks)

      val data = model.ReportData("out-sla-bugs-chart", MapValue(chart))

      owner ! ReportDataResponse(reportId, data)

  }
}
object BugsOutSlaChartActor {
  def props(owner: ActorRef, renderChart: Boolean = true) = Props(classOf[BugsOutSlaChartActor], owner, renderChart)

  def outSlaChartData(priority: String, marks: Seq[String], bugs: Seq[Bug]): Seq[(Int, String, String)] = {
    val grouped = bugs.groupBy(bug => bug.stats.openMonth)
    marks.map { mark =>
      grouped.get(mark) match {
        case Some(v) => (v.length, priority, mark)
        case None => (0, priority, mark)
      }
    }
  }

  def outSlaChart(marks: Seq[String], bugs: Seq[Bug]): ReportField = {
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

  def bugsOutSlaChart(bugs: Map[String, Seq[Bug]], marks: Seq[String]): ReportField = {
    val p1 = bugs.getOrElse(Metrics.P1Priority, Seq())
    val p2 = bugs.getOrElse(Metrics.P2Priority, Seq())
    val p1p2 = p1 ++ p2

    outSlaChart(marks, p1p2)
  }

  def bugsOutSlaData(bugs: Map[String, Seq[Bug]], marks: Seq[String]): ReportField = {
    val p1 = bugs.getOrElse(Metrics.P1Priority, Seq())
    val p2 = bugs.getOrElse(Metrics.P2Priority, Seq())
    val p1p2 = p1 ++ p2

    ReportField("datasets",
      MapValue(
        ReportField("labels",
          ListValue(
            marks.map(mark => StringValue(mark)): _*
          )
        ),
        ReportField("dataset",
          ListValue(
            bugsOutSlaDataSet(Metrics.P1Priority, marks, p1),
            bugsOutSlaDataSet(Metrics.P2Priority, marks, p2)
          )
        )
      )
    )

  }

  def bugsOutSlaDataSet(priority: String, marks: Seq[String], bugs: Seq[Bug]): ReportValue = {
    val data = outSlaChartData(priority, marks, bugs)

    MapValue(
      ReportField("name", StringValue(priority)),
      ReportField("values",
        ListValue(
          data.indices.map { idx =>
            MapValue(
              ReportField("x", IntValue(idx)),
              ReportField("y", IntValue(data(idx)._1))
            )
          }: _*
        )
      )
    )

  }
}