package bugapp.report

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

import akka.actor.{ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportActor.formatNumber
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.report.model.{ListValue, MapValue, ReportField, StringValue}
import bugapp.repository.{Bug, Employee, EmployeeRepository}


/**
  * @author Alexander Kuleshov
  */
class ReportersBugNumberByPeriodActor(owner: ActorRef, repository: EmployeeRepository, weeks: Int) extends ReportWorker(owner) with ActorLogging {
  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>

      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]
      val startDate = endDate.minusWeeks(weeks).truncatedTo(ChronoUnit.DAYS)

      val weeklyBugs = bugs.filter {bug => bug.actualDate.isAfter(startDate) && bug.actualDate.isBefore(endDate)}
      log.debug(s"Filtered bugs number: ${weeklyBugs.size}")

      val departmentBugs: Map[String, Map[String, Seq[Bug]]] = weeklyBugs.groupBy {bug =>
        repository.getEmployee(bug.reporter).getOrElse(Employee(bug.reporter, "Other")).department
      }.map { tuple =>
        val department = tuple._1
        val bugs = tuple._2
        (department, bugs.groupBy(bug => bug.actualStatus))
      }

      val reporterBugsValue = departmentBugs.toSeq.
        sortWith {(tuple1, tuple2) =>
          tuple1._2.values.map(_.size).sum > tuple2._2.values.map(_.size).sum
        }.map { tuple =>
          val department = tuple._1
          val bugs = tuple._2
          val closedBugsNumber = bugs.getOrElse(Metrics.FixedStatus, Seq()).length
          val invalidBugsNumber = bugs.getOrElse(Metrics.InvalidStatus, Seq()).length
          val openedBugsNumber = bugs.getOrElse(Metrics.OpenStatus, Seq()).length
          reporterBugsData(department, closedBugsNumber, invalidBugsNumber, openedBugsNumber)
        }

      val totalBugNumber = departmentBugs.map { tuple =>
        val bugs = tuple._2
        val closedBugsNumber = bugs.getOrElse(Metrics.FixedStatus, Seq()).length
        val invalidBugsNumber = bugs.getOrElse(Metrics.InvalidStatus, Seq()).length
        val openedBugsNumber = bugs.getOrElse(Metrics.OpenStatus, Seq()).length
        (closedBugsNumber, invalidBugsNumber, openedBugsNumber)
      }.foldLeft((0, 0, 0))((acc, tuple) => (acc._1 + tuple._1, acc._2 + tuple._2, acc._3 + tuple._3))

      val reporterBugsValues = reporterBugsValue :+ reporterBugsData("Grand Total", totalBugNumber._1, totalBugNumber._2, totalBugNumber._3)

      val data = model.ReportData(s"reporter-bugs-by-weeks-$weeks",
        MapValue(
          ReportField("reporter-bugs", ListValue(reporterBugsValues: _*))
        )
      )

      owner ! ReportDataResponse(reportId, data)
  }

  private def reporterBugsData(department: String, closed: Int, invalid: Int, opened: Int): MapValue = {
    MapValue(
      ReportField("reporter", StringValue(department)),
      ReportField("closed", StringValue(formatNumber(closed))),
      ReportField("invalid", StringValue(formatNumber(invalid))),
      ReportField("opened", StringValue(formatNumber(opened))),
      ReportField("total", StringValue(formatNumber(closed + invalid + opened)))
    )
  }
}

object ReportersBugNumberByPeriodActor {
  def props(owner: ActorRef, repository: EmployeeRepository, weeks: Int) = Props(classOf[ReportersBugNumberByPeriodActor], owner, repository, weeks)
}
