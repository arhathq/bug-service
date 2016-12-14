package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import bugapp.BugApp
import bugapp.repository.{Bug, FileEmployeeRepository}

import scala.collection.mutable
import scala.xml.Elem

/**
  * Created by arhathq on 04.08.2016.
  */
class ReportDataBuilder(reportActor: ActorRef) extends Actor with ActorLogging {
  import ReportDataBuilder._

  private val jobs = mutable.Map.empty[String, Set[ActorRef]]
  private val requests = mutable.Map.empty[String, Map[String, Any]]
  private val data = mutable.Map.empty[String, List[Elem]]

  private val employeeRepository = new FileEmployeeRepository

  def createWorkers(reportType: String): Set[ActorRef] = reportType match {
    case "weekly" => Set(
      context.actorOf(AllOpenBugsNumberByPriorityActor.props(self)),
      context.actorOf(OpenTopBugListActor.props(self)),
      context.actorOf(ReportersBugNumberByPeriodActor.props(self, employeeRepository)),
      context.actorOf(ReportersBugNumberByThisWeekActor.props(self, employeeRepository)),
      context.actorOf(PrioritizedBugNumberByThisWeekActor.props(self)),
      context.actorOf(OpenBugsNumberByProductActor.props(self)),
      context.actorOf(BugsByPeriodChartActor.props(self)),
      context.actorOf(TopAssigneesActor.props(self)),
      context.actorOf(WeeklySummaryReportActor.props(self))
    )
    case "sla" => Set(
      context.actorOf(SlaReportActor.props(self)),
      context.actorOf(BugsOutSlaActor.props(self))
    )
    case _ => Set()
  }

  override def receive: Receive = {
    case GetReportData(reportId, reportParams, bugs) =>
      val reportType: String = reportParams(ReportParams.ReportType).asInstanceOf[String]
      val workers = createWorkers(reportType)
      jobs += reportId -> workers
      requests += reportId -> reportParams
      log.info(s"Job [$reportId], Workers $jobs")
      workers.foreach(_ ! ReportDataRequest(reportId, reportParams, bugs))

    case ReportDataResponse(reportId, result) =>
      data.get(reportId) match {
        case Some(reportDataList) => data += reportId -> (result :: reportDataList)
        case None => data += reportId -> (result :: Nil)
      }
      jobs.get(reportId) match {
        case Some(workers) =>
          sender ! PoisonPill
          val busyWorkers = workers - sender
          if (busyWorkers.isEmpty) {
            val reportData = buildReportData(reportId)
            jobs -= reportId
            requests -= reportId
            reportActor ! reportData
          } else
            jobs += reportId -> busyWorkers
        case None =>
      }
  }

  def buildReportData(reportId: String): ReportData = {
    val reportType = requests(reportId)(ReportParams.ReportType).asInstanceOf[String]
    val excludedComponents = requests(reportId)(ReportParams.ExcludedComponents).asInstanceOf[Seq[String]]

    data.get(reportId) match {
      case Some(dataList) =>
        val reportData = new xml.NodeBuffer()
        dataList.foreach(reportData.append)
        data -= reportId

        val result =
          <bug-reports>
            <report-header>
              <date>{BugApp.toDate}</date>
            </report-header>
            {reportData}
            <report-footer>
              <note>{createNote(excludedComponents)}</note>
            </report-footer>
          </bug-reports>

        ReportData(reportId, reportType, result)
    }
  }

  def createNote(excludedComponents: Seq[String]): String = {
    if (excludedComponents.nonEmpty)
      s"* Bugs ${excludedComponents.mkString("\"", "\", \"", "\"")} excluded from report"
    else
      ""
  }
}

object ReportDataBuilder {
  case class GetReportData(reportId: String, reportParams: Map[String, Any], bugs: Seq[Bug])
  case class ReportData(reportId: String, reportType: String, result: Elem)

  case class ReportDataRequest(reportId: String, reportParams: Map[String, Any], bugs: Seq[Bug])
  case class ReportDataResponse(reportId: String, result: Elem)

  def props(reportActor: ActorRef) = Props(classOf[ReportDataBuilder], reportActor)
}