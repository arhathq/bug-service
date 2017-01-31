package bugapp.report

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportActor.formatNumber
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.report.model.{ListValue, MapValue, ReportField, StringValue}
import bugapp.repository.Bug
import org.jfree.data.category.DefaultCategoryDataset

/**
  * @author Alexander Kuleshov
  */
class BugsByPeriodChartActor(owner: ActorRef, weeks: Int) extends Actor with ActorLogging {
  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>

      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]
      val startDate = endDate.minusWeeks(weeks).truncatedTo(ChronoUnit.DAYS)

      val filteredBugs = bugs.filter {bug => bug.actualDate.isAfter(startDate) && bug.actualDate.isBefore(endDate)}
      log.debug(s"Filtered bugs number: ${filteredBugs.size}")

      val weeklyBugs: Map[String, Map[String, Seq[Bug]]] = filteredBugs.groupBy { bug =>
        Metrics.weekFormat(bug.actualDate)
      }. map { tuple =>
        val week = tuple._1
        val bugs = tuple._2
        (week, bugs.groupBy(bug => bug.stats.status))
      }

      val totalBugNumber = weeklyBugs.map { tuple =>
        val bugs = tuple._2
        val closedBugsNumber = bugs.getOrElse(Metrics.FixedStatus, Seq()).length
        val invalidBugsNumber = bugs.getOrElse(Metrics.InvalidStatus, Seq()).length
        val openedBugsNumber = bugs.getOrElse(Metrics.OpenStatus, Seq()).length
        (closedBugsNumber, invalidBugsNumber, openedBugsNumber)
      }.foldLeft((0, 0, 0))((acc, tuple) => (acc._1 + tuple._1, acc._2 + tuple._2, acc._3 + tuple._3))
      log.debug(s"Total bug number: $totalBugNumber")

      val marks = Metrics.marksByDates(startDate, endDate)

      val data =
        model.ReportData(s"bugs-by-weeks-$weeks",
          MapValue(
            weeklyBugsData(marks, weeklyBugs),
            chartData(marks, weeklyBugs)
          )
        )

      owner ! ReportDataResponse(reportId, data)
  }

  private def weeklyBugsData(marks: Seq[String], bugs: Map[String, Map[String, Seq[Bug]]]): ReportField = {
    val marksData = ReportField("header",
      MapValue(
        marks.map(week => ReportField(s"w$week", StringValue(week))): _*
      )
    )

    val fixed = weeklyBugsData(Metrics.FixedStatus, marks, bugs)
    val fixedData = ReportField("row",
      MapValue(
        ReportField("name", StringValue("Closed")),
        ReportField("value",
          ListValue(
            fixed.map(tuple => StringValue(formatNumber(tuple._2))): _*
          )
        )
      )
    )

    val open = weeklyBugsData(Metrics.OpenStatus, marks, bugs)
    val openData = ReportField("row",
      MapValue(
        ReportField("name", StringValue("Open")),
        ReportField("value",
          ListValue(
            open.map(tuple => StringValue(formatNumber(tuple._2))): _*
          )
        )
      )
    )

    val invalid = weeklyBugsData(Metrics.InvalidStatus, marks, bugs)
    val invalidData = ReportField("row",
      MapValue(
        ReportField("name", StringValue("Invalid")),
        ReportField("value",
          ListValue(
            invalid.map(tuple => StringValue(formatNumber(tuple._2))): _*
          )
        )
      )
    )

    ReportField("weekly-bugs",
      MapValue(
        marksData,
        openData,
        fixedData,
        invalidData
      )
    )
  }

  private def weeklyBugsData(priority: String, marks: Seq[String], bugs: Map[String, Map[String, Seq[Bug]]]): Seq[(String, Int)] = {
    marks.map { mark =>
      val bugsByStatus = bugs.getOrElse(mark, Map())
      val bugsNumber = bugsByStatus.getOrElse(priority, Seq()).length
      (mark, bugsNumber)
    }
  }

  private def chartData(marks: Seq[String], bugs: Map[String, Map[String, Seq[Bug]]]): ReportField = {
    val dataSet = new DefaultCategoryDataset()

    marks.foreach { mark =>
      val bugsByStatus = bugs.getOrElse(mark, Map())
      dataSet.addValue(bugsByStatus.getOrElse(Metrics.InvalidStatus, Seq()).length, "Invalid", mark)
      dataSet.addValue(bugsByStatus.getOrElse(Metrics.FixedStatus, Seq()).length, "Closed", mark)
      dataSet.addValue(bugsByStatus.getOrElse(Metrics.OpenStatus, Seq()).length, "Open", mark)
    }

    ReportField("image",
      MapValue(
        ReportField("content-type", StringValue("image/jpeg")),
        ReportField("content-value", StringValue(ChartGenerator.generateBase64BugsFromLast15Weeks1(dataSet)))
      )
    )
  }
}

object BugsByPeriodChartActor {
  def props(owner: ActorRef, weeks: Int) = Props(classOf[BugsByPeriodChartActor], owner, weeks)
}
