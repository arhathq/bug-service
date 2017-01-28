package bugapp.report

import java.time.{DayOfWeek, LocalDate, OffsetDateTime}
import java.time.temporal.ChronoUnit

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.report.model.{MapValue, ReportField, StringValue}
import bugapp.repository.{Bug, BugHistory, HistoryItemChange}
import org.jfree.data.category.DefaultCategoryDataset


/**
  *
  */
class WeeklySummaryChartActor(owner: ActorRef) extends Actor with ActorLogging {
  import WeeklySummaryChartActor._

  override def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]
      val startDate = endDate.minusDays(7).truncatedTo(ChronoUnit.DAYS)
      val bugsForLastWeek = bugs.filter { bug =>
        (bug.stats.status == Metrics.OpenStatus && bug.opened.isAfter(startDate) && bug.opened.isBefore(endDate)) ||
          (bug.changed.isAfter(startDate) && bug.changed.isBefore(endDate))
      }

      val data =
        model.ReportData("week-summary-report-chart",
          MapValue(
            periodData(startDate, endDate),
            weekSummaryChart(startDate, endDate, bugsForLastWeek)
          )
        )

      owner ! ReportDataResponse(reportId, data)
  }

  private def weekSummaryChartData(status: String, marks: Seq[OffsetDateTime], bugs: Seq[Bug], event: (Bug) => Seq[BugEvent]): Seq[(Int, String, String)] = {
    val events = bugs.flatMap(bug => bugEvents(bug) ++ event(bug))

    val grouped = events.groupBy(event => event.eventDate)
    marks.map { mark =>
      grouped.get(mark.toLocalDate) match {
        case Some(v) =>
          val events = v.filter(ev => ev.status == status)
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
    weekSummaryChartData(Metrics.OpenStatus, marks, bugs, (b: Bug) => {
      if (b.opened.isAfter(startDate))
        Seq(BugEvent(b.id, b.opened.toLocalDate, "NEW", "newBug"))
      else Seq()
    }).foreach(data => dataSet.addValue(data._1, data._2, data._3))
    weekSummaryChartData(Metrics.FixedStatus, marks, bugs, (_: Bug) => Seq()).foreach(data => dataSet.addValue(data._1, data._2, data._3))

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

}

object WeeklySummaryChartActor {
  def props(owner: ActorRef) = Props(classOf[WeeklySummaryChartActor], owner)

  def bugEvents(bug: Bug): Seq[BugEvent] = {
    bug.history.getOrElse(BugHistory(bug.id, None, Seq())).items.
      flatMap(item => item.changes.
        filter(
          change =>
            (change.field == "status" && (change.added == "RESOLVED" || change.added == "REOPENED")) ||
              ((change.added == "Production" || change.removed == "Dataload Failed" ||
                change.removed == "New Files Arrived" || change.removed == "Data Consistency")
                && isNotResolved(bug))
        ).
        map {
          case change @ HistoryItemChange(_, "RESOLVED", "status") => BugEvent(bug.id, item.when.toLocalDate, "RESOLVED", "resolved", change)
          case change @ HistoryItemChange(_, "REOPENED", "status") => BugEvent(bug.id, item.when.toLocalDate, "REOPENED", "reopened", change)
          case change @ HistoryItemChange(_, "Production", _) if isNotResolved(bug) => BugEvent(bug.id, item.when.toLocalDate, bug.status, "movedToQueue", change)
          case change @ HistoryItemChange("Dataload Failed", _, _) if isNotResolved(bug) => BugEvent(bug.id, item.when.toLocalDate, bug.status, "movedToQueue", change)
          case change @ HistoryItemChange("New Files Arrived", _, _) if isNotResolved(bug) => BugEvent(bug.id, item.when.toLocalDate, bug.status, "movedToQueue", change)
          case change @ HistoryItemChange("Data Consistency", _, _) if isNotResolved(bug) => BugEvent(bug.id, item.when.toLocalDate, bug.status, "movedToQueue", change)
        }
      )
  }

  def isNotResolved(bug: Bug): Boolean = {
    bug.status != "RESOLVED" && bug.status != "VERIFIED" && bug.status != "CLOSED"
  }

  case class BugEvent(bugId: Int, eventDate: LocalDate, bugStatus: String, event: String, source: Option[HistoryItemChange] = None) {
    def status(): String = event match {
      case "newBug" => Metrics.OpenStatus
      case "resolved" => Metrics.FixedStatus
      case "reopened" => Metrics.OpenStatus
      case "movedToQueue" => Metrics.OpenStatus
    }
  }
  object BugEvent {
    def apply(bugId: Int, eventDate: LocalDate, status: String, event: String, source: HistoryItemChange): BugEvent =
      BugEvent(bugId, eventDate, status, event, Some(source))
  }
}