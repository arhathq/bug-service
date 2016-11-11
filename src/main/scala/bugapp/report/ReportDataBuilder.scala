package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import bugapp.BugApp
import bugapp.repository.Bug

import scala.collection.mutable
import scala.xml.Elem

/**
  * Created by arhathq on 04.08.2016.
  */
class ReportDataBuilder(reportActor: ActorRef) extends Actor with ActorLogging {
  import ReportDataBuilder._

  private val jobs = mutable.Map.empty[String, Set[ActorRef]]
  private val data = mutable.Map.empty[String, (String, List[Elem])]

  def createWorkers(reportType: String): Set[ActorRef] = reportType match {
    case "weekly" => Set(context.actorOf(AllOpenBugsReportActor.props(self)))
    case "sla" => Set(context.actorOf(SlaReportActor.props()))
    case _ => Set()
  }

  override def receive: Receive = {
    case GetReportData(reportId, reportParams, bugs) =>
      val reportType: String = reportParams(ReportParams.ReportType).asInstanceOf[String]
      val workers = createWorkers(reportType)
      jobs += reportId -> workers
      log.info(s"Job [$reportId], Workers $jobs")
      workers.foreach(_ ! ReportDataRequest(reportId, reportParams, bugs))

    case ReportDataResponse(reportData) =>
      val reportId = reportData.reportId
      data.get(reportData.reportId) match {
        case Some(reportDataList) => data += reportId -> ((reportData.reportType, reportData.result :: reportDataList._2))
        case None => data += reportId -> (reportData.reportType, reportData.result :: Nil)
      }
      jobs.get(reportId) match {
        case Some(workers) =>
          sender ! PoisonPill
          val busyWorkers = workers - sender
          if (busyWorkers.isEmpty) {
            jobs -= reportId
            reportActor ! ReportDataResponse(buildReportData(reportId))
          } else
            jobs += reportId -> busyWorkers
        case None =>
      }
  }

  def buildReportData(reportId: String): ReportData = {
    data.get(reportId) match {
      case Some((reportType, dataList)) =>
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
              <note>* Bugs "CRF Hot Deploy - Prod DB", "Ecomm Deploy - Prod DB", "Dataload Failed", "New Files Arrived", "Data Consistency" excluded from report</note>
            </report-footer>
          </bug-reports>

        ReportData(reportId, reportType, result)
    }
  }
}

object ReportDataBuilder {
  case class GetReportData(reportId: String, reportParams: Map[String, Any], bugs: Seq[Bug])

  case class ReportDataRequest(reportId: String, reportParams: Map[String, Any], bugs: Seq[Bug])
  case class ReportDataResponse(reportData: ReportData)

  case class ReportData(reportId: String, reportType: String, result: Elem)

  def props(reportActor: ActorRef) = Props(classOf[ReportDataBuilder], reportActor)
}