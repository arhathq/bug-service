package bugapp.report

import java.time.{DayOfWeek, OffsetDateTime}
import java.time.temporal.Temporal

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportActor.createXmlElement
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.repository.Bug
import org.jfree.data.category.DefaultCategoryDataset

import scala.xml.{Elem, Group, Text}

/**
  *
  */
class WeeklySummaryChartActor(owner: ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]
      val startDate = endDate.minusDays(7)

      val data =
        <week-summary-report-chart>
          {periodElem(startDate, endDate)}
          {weekSummaryChart(startDate, endDate, bugs)}
        </week-summary-report-chart>

      owner ! ReportDataResponse(reportId, data)
  }

  private def weekSummaryChartData(status: String, marks: Seq[OffsetDateTime], bugs: Seq[Bug], groupBy: (Bug) => Temporal): Seq[(Int, String, String)] = {
    val grouped = bugs.groupBy(groupBy)
    marks.map { mark =>
      grouped.get(mark.toLocalDate) match {
        case Some(v) => (v.length, status, ReportActor.dateFormat.format(mark))
        case None => (0, status, ReportActor.dateFormat.format(mark))
      }
    }
  }

  private def weekSummaryChart(startDate: OffsetDateTime, endDate: OffsetDateTime, bugs: Seq[Bug]): Elem = {
    val lastResolvedAndOpenedBugs = bugs.filter { bug =>
      (bug.stats.status == Metrics.OpenStatus && bug.opened.isAfter(startDate)) ||
        (bug.stats.status == Metrics.FixedStatus && bug.changed.isAfter(startDate))
    }
    val marks = Metrics.daysRange(startDate, endDate)

    val dataSet = new DefaultCategoryDataset()
    weekSummaryChartData(Metrics.OpenStatus, marks, lastResolvedAndOpenedBugs, (b: Bug) => b.opened.toLocalDate).foreach(data => dataSet.addValue(data._1, data._2, data._3))
    weekSummaryChartData(Metrics.FixedStatus, marks, lastResolvedAndOpenedBugs, (b: Bug) => b.changed.toLocalDate).foreach(data => dataSet.addValue(data._1, data._2, data._3))

    <image>
      <content-type>image/jpeg</content-type>
      <content-value>{ChartGenerator.generateBase64NewVsResolvedBugs(dataSet)}</content-value>
    </image>
  }

  private def periodElem(startDate: OffsetDateTime, endDate: OffsetDateTime): Elem = {
    val days = Metrics.daysRange(startDate, endDate)
    val weekends = days.filter(day => day.getDayOfWeek == DayOfWeek.SATURDAY || day.getDayOfWeek == DayOfWeek.SUNDAY)

    val weekendElements = weekends.zipWithIndex.map {tuple =>
      val date = tuple._1
      val index = tuple._2
      createXmlElement(s"weekend${index + 1}", Text(ReportActor.dateTimeFormat.format(date)))
    }

    createXmlElement("period",
      <from>{ReportActor.dateTimeFormat.format(startDate)}</from>,
      <to>{ReportActor.dateTimeFormat.format(endDate)}</to>,
      Group(weekendElements)
    )
  }

}

object WeeklySummaryChartActor {
  def props(owner: ActorRef) = Props(classOf[WeeklySummaryChartActor], owner)
}
