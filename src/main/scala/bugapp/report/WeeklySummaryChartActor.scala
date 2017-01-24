package bugapp.report

import java.time.{DayOfWeek, OffsetDateTime}
import java.time.temporal.ChronoUnit

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.report.model.{MapValue, ReportField, StringValue}
import bugapp.repository.{Bug, BugHistory}
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

  private def bugEvents(bug: Bug): Seq[BugEvent] = {
    bug.history.getOrElse(BugHistory(bug.id, None, Seq())).items.
      flatMap(item => item.changes.
        filter(change => change.field == "status" && (change.added == "RESOLVED" || change.added == "REOPENED")).
        map(change => BugEvent(bug.id, item.when, status(change.added))))
  }

  private def weekSummaryChartData(status: String, marks: Seq[OffsetDateTime], bugs: Seq[Bug], event: (Bug) => Seq[BugEvent]): Seq[(Int, String, String)] = {
    val events = bugs.flatMap(bug => bugEvents(bug) ++ event(bug))

    val grouped = events.groupBy(event => event.eventDate.toLocalDate)
    marks.map { mark =>
      grouped.get(mark.toLocalDate) match {
        case Some(v) => val events = v.filter(ev => ev.event == status); (events.length, status, ReportActor.dateFormat.format(mark))
        case None => (0, status, ReportActor.dateFormat.format(mark))
      }
    }
  }

  private def weekSummaryChart(startDate: OffsetDateTime, endDate: OffsetDateTime, bugs: Seq[Bug]): ReportField = {
/*
    val lastOpenedBugs = bugs.filter { bug =>
      bug.stats.status == Metrics.OpenStatus && bug.opened.isAfter(startDate) && bug.opened.isBefore(endDate)
    }
    // debug code
    lastOpenedBugs.groupBy(bug => bug.opened.toLocalDate).foreach {tuple =>
      val date = tuple._1
      val bugs = tuple._2
      log.debug(s"$date")
      bugs.foreach(bug => log.debug(s"[${bug.id}]: ${bug.opened} [${bug.status} / ${bug.resolution}] (${bug.stats.status})"))
    }

    val lastResolvedBugs = bugs.filter { bug =>
      (bug.stats.status == Metrics.FixedStatus && bug.stats.resolvedTime.isDefined
        && bug.stats.resolvedTime.get.isAfter(startDate) && bug.stats.resolvedTime.get.isBefore(endDate))
    }
    // debug code
    lastResolvedBugs.groupBy(bug => bug.stats.resolvedTime.get.toLocalDate).foreach {tuple =>
      val date = tuple._1
      val bugs = tuple._2
      log.debug(s"$date")
      bugs.foreach(bug => log.debug(s"[${bug.id}]: ${bug.changed} [${bug.status} / ${bug.resolution}] (${bug.stats.status})"))
    }
*/

    val marks = Metrics.daysRange(startDate, endDate)

    val dataSet = new DefaultCategoryDataset()
    weekSummaryChartData(Metrics.OpenStatus, marks, bugs, (b: Bug) => {
      if (b.opened.isAfter(startDate)) Seq(BugEvent(b.id, b.opened, Metrics.OpenStatus))
      else Seq()
    }).foreach(data => dataSet.addValue(data._1, data._2, data._3))
    weekSummaryChartData(Metrics.FixedStatus, marks, bugs, (b: Bug) => Seq()).foreach(data => dataSet.addValue(data._1, data._2, data._3))

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

  case class BugEvent(bugId: Int, eventDate: OffsetDateTime, event: String)

  def status(event: String): String = event match {
    case "RESOLVED" => Metrics.FixedStatus
    case "REOPENED" => Metrics.OpenStatus
    case _ => "UNDEFINED"
  }
}
