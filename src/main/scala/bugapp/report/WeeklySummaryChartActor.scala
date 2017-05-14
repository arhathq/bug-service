package bugapp.report

import java.time.{DayOfWeek, OffsetDateTime}
import java.time.temporal.ChronoUnit

import akka.actor.{ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilderActor.{ReportDataRequest, ReportDataResponse}
import bugapp.report.model._
import bugapp.repository._
import org.jfree.data.category.DefaultCategoryDataset


/**
  *
  */
class WeeklySummaryChartActor(owner: ActorRef, renderChart: Boolean) extends ReportWorker(owner) with ActorLogging {

  override def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]
      val startDate = endDate.minusDays(7).truncatedTo(ChronoUnit.DAYS)
      val bugsForLastWeek = bugs.filter { bug =>
        (bug.actualStatus == Metrics.OpenStatus && bug.opened.isAfter(startDate) && bug.opened.isBefore(endDate)) ||
          (bug.changed.isAfter(startDate) && bug.changed.isBefore(endDate))
      }

      val chart =
        if(renderChart)
          weekSummaryChart(startDate, endDate, bugsForLastWeek)
        else
          weekSummaryData(startDate, endDate, bugsForLastWeek)

      val data =
        model.ReportData("week-summary-report-chart",
          MapValue(
            periodData(startDate, endDate),
            chart
          )
        )

      owner ! ReportDataResponse(reportId, data)
  }

  private def weekSummaryChartData(status: String, marks: Seq[OffsetDateTime], bugs: Seq[Bug]): Seq[(Int, String, String)] = {
    val events = bugs.flatMap(bug => bug.events.filter {
      case _: BugCreatedEvent => true
      case _: BugResolvedEvent => true
      case _: BugReopenedEvent => true
      case _: BugMarkedAsProductionEvent if bug.isNotResolved => true
      case BugComponentChangedEvent(_, _, _, "Dataload Failed", _) |
           BugComponentChangedEvent(_, _, _, "New Files Arrived", _) |
           BugComponentChangedEvent(_, _, _, "Data Consistency", _)
        if bug.isNotResolved => true
      case _ => false
    })

    val grouped = events.groupBy(event => event.date.toLocalDate)
    marks.map { mark =>
      grouped.get(mark.toLocalDate) match {
        case Some(v) =>
          val events = v.filter {
            case _: BugCreatedEvent if status == Metrics.OpenStatus => true
            case _: BugResolvedEvent if status == Metrics.FixedStatus => true
            case _: BugReopenedEvent if status == Metrics.OpenStatus => true
            case _: BugMarkedAsProductionEvent if status == Metrics.OpenStatus => true
            case _: BugComponentChangedEvent if status == Metrics.OpenStatus => true
            case _ => false
          }
//          log.debug(s"$mark")
//          events.foreach(e => log.debug(s"$e"))
          (events.size, status, ReportActor.dateFormat.format(mark))
        case None => (0, status, ReportActor.dateFormat.format(mark))
      }
    }
  }

  private def weekSummaryChart(startDate: OffsetDateTime, endDate: OffsetDateTime, bugs: Seq[Bug]): ReportField = {
    val marks = Metrics.daysRange(startDate, endDate)

    val dataSet = new DefaultCategoryDataset()
    weekSummaryChartData(Metrics.OpenStatus, marks, bugs).foreach(data => dataSet.addValue(data._1, data._2, data._3))
    weekSummaryChartData(Metrics.FixedStatus, marks, bugs).foreach(data => dataSet.addValue(data._1, data._2, data._3))

    ReportField("image",
      MapValue(
        ReportField("content-type", StringValue("image/jpeg")),
        ReportField("content-value", StringValue(ChartGenerator.generateBase64NewVsResolvedBugs(dataSet)))
      )
    )
  }

  private def periodData(startDate: OffsetDateTime, endDate: OffsetDateTime): ReportField = {
    val days = Metrics.daysRange(startDate, endDate)
    val weekends = days.filter(day => day.getDayOfWeek == DayOfWeek.SATURDAY || day.getDayOfWeek == DayOfWeek.SUNDAY)

    val weekendData = weekends.zipWithIndex.map {tuple =>
      val date = tuple._1
      val index = tuple._2
      ReportField(s"weekend${index + 1}", StringValue(ReportActor.dateTimeFormat.format(date)))
    }

    ReportField("period",
      MapValue(
        Seq(
          ReportField("from", StringValue(ReportActor.dateTimeFormat.format(startDate))),
          ReportField("to", StringValue(ReportActor.dateTimeFormat.format(endDate)))
        ) ++ weekendData: _*
      )
    )
  }

  private def weekSummaryData(startDate: OffsetDateTime, endDate: OffsetDateTime, bugs: Seq[Bug]): ReportField = {
    val marks = Metrics.daysRange(startDate, endDate)

    val openData = weekSummaryChartData(Metrics.OpenStatus, marks, bugs)
    val fixedData = weekSummaryChartData(Metrics.FixedStatus, marks, bugs)


    ReportField("datasets",
      MapValue(
        ReportField("labels",
          ListValue(
            marks.map(mark => StringValue(mark.toLocalDate.toString)): _*
          )
        ),
        ReportField("dataset",
          ListValue(
            weekSummaryDataSet(Metrics.OpenStatus, marks, openData),
            weekSummaryDataSet(Metrics.FixedStatus, marks, fixedData)
          )
        )
      )
    )

  }

  private def weekSummaryDataSet(status: String, marks: Seq[OffsetDateTime], data: Seq[(Int, String, String)]): ReportValue = {
    MapValue(
      ReportField("name", StringValue(status)),
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

object WeeklySummaryChartActor {
  def props(owner: ActorRef, renderChart: Boolean = true) = Props(classOf[WeeklySummaryChartActor], owner, renderChart)
}