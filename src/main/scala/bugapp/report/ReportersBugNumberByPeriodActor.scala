package bugapp.report

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportActor.formatNumber
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.repository.{Bug, Employee, EmployeeRepository}

import scala.xml._

/**
  * @author Alexander Kuleshov
  */
class ReportersBugNumberByPeriodActor(owner: ActorRef, repository: EmployeeRepository, weeks: Int) extends Actor with ActorLogging {
  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>

      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]
      val startDate = endDate.minusWeeks(weeks)

      val weeklyBugs = bugs.filter(bug => bug.opened.isAfter(startDate))

      val departmentBugs: Map[String, Map[String, Seq[Bug]]] = weeklyBugs.groupBy {bug =>
        repository.getEmployee(bug.reporter).getOrElse(Employee(bug.reporter, "Other")).department
      }.map { tuple =>
        val department = tuple._1
        val bugs = tuple._2
        (department, bugs.groupBy(bug => bug.stats.status))
      }

      val reporterBugsElems = departmentBugs.map { tuple =>
        val department = tuple._1
        val bugs = tuple._2
        val closedBugsNumber = bugs.getOrElse(Metrics.FixedStatus, Seq()).length
        val invalidBugsNumber = bugs.getOrElse(Metrics.InvalidStatus, Seq()).length
        val openedBugsNumber = bugs.getOrElse(Metrics.OpenStatus, Seq()).length
        reporterBugsElem(department, closedBugsNumber, invalidBugsNumber, openedBugsNumber)
      }.toSeq

      val totalBugNumber = departmentBugs.map { tuple =>
        val bugs = tuple._2
        val closedBugsNumber = bugs.getOrElse(Metrics.FixedStatus, Seq()).length
        val invalidBugsNumber = bugs.getOrElse(Metrics.InvalidStatus, Seq()).length
        val openedBugsNumber = bugs.getOrElse(Metrics.OpenStatus, Seq()).length
        (closedBugsNumber, invalidBugsNumber, openedBugsNumber)
      }.foldLeft((0, 0, 0))((acc, tuple) => (acc._1 + tuple._1, acc._2 + tuple._2, acc._3 + tuple._3))

      val data =
        Elem.apply(null, s"reporter-bugs-by-weeks-$weeks", Null, TopScope, true,
          Group(reporterBugsElems),
          reporterBugsElem("Grand Total", totalBugNumber._1, totalBugNumber._2, totalBugNumber._3))

      owner ! ReportDataResponse(reportId, data)
  }

  private def reporterBugsElem(department: String, closed: Int, invalid: Int, opened: Int): Elem = {
    <reporter-bugs>
      <reporter>{department}</reporter>
      <closed>{formatNumber(closed)}</closed>
      <invalid>{formatNumber(invalid)}</invalid>
      <opened>{formatNumber(opened)}</opened>
      <total>{formatNumber(closed + invalid + opened)}</total>
    </reporter-bugs>
  }
}

object ReportersBugNumberByPeriodActor {
  def props(owner: ActorRef, repository: EmployeeRepository, weeks: Int) = Props(classOf[ReportersBugNumberByPeriodActor], owner, repository, weeks)
}
