package bugapp.report

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

import akka.actor.{ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilderActor.{ReportDataRequest, ReportDataResponse}
import bugapp.report.model.{ReportField, _}
import bugapp.repository.Bug
import org.jfree.data.category.DefaultCategoryDataset


/**
  *
  */
class SlaReportActor(owner: ActorRef) extends ReportWorker(owner) with ActorLogging {
  import bugapp.report.SlaReportActor._

  override def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val weekPeriod = reportParams(ReportParams.WeekPeriod).asInstanceOf[Int]
      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]
      val startDate = endDate.minusWeeks(weekPeriod - 1).truncatedTo(ChronoUnit.DAYS)
      val bugtrackerUri = reportParams(ReportParams.BugtrackerUri).asInstanceOf[String]

      val marks = Metrics.marks(endDate, weekPeriod)
      val actualBugs = bugs.filter(bugsForPeriod(_, startDate))
      val outSlaBugs = bugsOutSla(actualBugs)
      val p1OutSla = outSlaBugs.getOrElse(Metrics.P1Priority, Seq())
      val p2OutSla = outSlaBugs.getOrElse(Metrics.P2Priority, Seq())

      val data =
        model.ReportData("sla",
          MapValue(
            ReportField("p1-sla",
              MapValue(
                sla(Metrics.P1Priority, marks, p1OutSla, actualBugs.filter(_.priority == Metrics.P1Priority))
              )
            ),
            ReportField("p2-sla",
              MapValue(
                sla(Metrics.P2Priority, marks, p2OutSla, actualBugs.filter(_.priority == Metrics.P2Priority))
              )
            )
          )
        )

      owner ! ReportDataResponse(reportId, data)
  }

  def slaAchievementTrendChartData(priority: String, marks:Seq[String], bugs: Seq[Bug]): Seq[(Double, String, String)] = {
    val grouped = bugs.groupBy(bug => bug.openMonth)
    marks.map { mark =>
      grouped.get(mark) match {
        case Some(v) => (slaPercentage(v.count(_.passSla), v.length), priority, mark)
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

  def sla(priority: String, marks:Seq[String], bugsOutSla: Seq[Bug], allBugs: Seq[Bug]): ReportField = {
    val outSlaGrouped = bugsOutSla.groupBy(bug => bug.openMonth)
    val allGrouped = allBugs.groupBy(bug => bug.openMonth)
    val res = marks.map(mark => outSlaGrouped.get(mark) match {
      case Some(v) =>
        weekPeriodElem(priority, mark, v.length, allGrouped.getOrElse(mark, Seq()).length)
      case None =>
        weekPeriodElem(priority, mark, 0, allGrouped.getOrElse(mark, Seq()).length)

    })
    val total =
      weekPeriodElem(priority, "Grand Total", outSlaGrouped.map(v => v._2.length).sum, allBugs.length)

    ReportField(s"sla-achievement",
      MapValue(
        res :+ total: _*
      )
    )
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
    bugs.groupBy(bug => bug.resolvedPeriod)
  }

  val bugsOutSla: (Seq[Bug]) => Map[String, Seq[Bug]] = bugs => {
    bugs.groupBy(bug => bug.priority match {
      case Metrics.P1Priority if !bug.passSla => Metrics.P1Priority
      case Metrics.P2Priority if !bug.passSla => Metrics.P2Priority
      case _ => "sla"
    })
  }

  val slaPercentage: (Int, Int) => Double = (count, totalCount) => if (totalCount < 1) 100.0 else count * 100.0 / totalCount

  val weekPeriodElem: (String, String, Int, Int) => ReportField = (priority, mark, outSlaCount, totalCount) => {
    ReportField("week-period",
      MapValue(
        ReportField("priority", StringValue(priority)),
        ReportField("week", StringValue(mark)),
        ReportField("slaPercentage", BigDecimalValue(slaPercentage(totalCount - outSlaCount, totalCount))),
        ReportField("slaCount", IntValue(totalCount - outSlaCount)),
        ReportField("outSlaCount", IntValue(outSlaCount)),
        ReportField("totalCount", IntValue(totalCount))
      )
    )
  }
}