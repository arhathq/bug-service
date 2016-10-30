package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import bugapp.BugApp
import bugapp.report.ReportDataBuilder.{GetReportData, ReportDataRequest, ReportDataResponse}
import bugapp.repository.Bug

import scala.collection.mutable
import scala.xml.Elem

/**
  * Created by arhathq on 04.08.2016.
  */
class ReportDataBuilder(reportActor: ActorRef) extends Actor with ActorLogging {

  private val jobs = mutable.Map.empty[String, Set[ActorRef]]
  private val data = mutable.Map.empty[String, List[Elem]]

  def createWorkers(reportType: String): Set[ActorRef] = reportType match {
    case "weekly" => Set(context.actorOf(AllOpenBugsReportActor.props(self)))
    case "sla" => Set(context.actorOf(SlaReportActor.props()))
    case _ => Set()
  }

  override def receive: Receive = {
    case GetReportData(reportId, reportType, bugs) =>
      val workers = createWorkers(reportType)
      jobs += reportId -> workers
      log.info(s"Job [$reportId], Workers $jobs")
      workers.foreach(_ ! ReportDataRequest(reportId, bugs))

    case ReportDataResponse(reportId, reportData) =>
      data.get(reportId) match {
        case Some(reportDataList) => data += reportId -> (reportData :: reportDataList)
        case None => data += reportId -> (reportData :: Nil)
      }
      jobs.get(reportId) match {
        case Some(workers) =>
          sender ! PoisonPill
          val busyWorkers = workers - sender
          if (busyWorkers.isEmpty) {
            jobs -= reportId
            reportActor ! ReportDataResponse(reportId, buildReportData(reportId))
          } else
            jobs += reportId -> busyWorkers
        case None =>
      }
  }

  def buildReportData(reportId: String): Elem = {
    val reportData = new xml.NodeBuffer()
    data.get(reportId) match {
      case Some(dataList) =>
        dataList.foreach(reportData.append)
        data -= reportId
      case None =>
    }

    <bug-reports>
      <report-header>
        <date>{BugApp.toDate}</date>
      </report-header>
      {reportData}
      <report-footer>
        <note>* Bugs "CRF Hot Deploy - Prod DB", "Ecomm Deploy - Prod DB", "Dataload Failed", "New Files Arrived", "Data Consistency" excluded from report</note>
      </report-footer>
    </bug-reports>
  }
}

object ReportDataBuilder {
  case class GetReportData(reportId: String, reportType: String, bugs: Seq[Bug])

  case class ReportDataRequest(reportId: String, bugs: Seq[Bug])
  case class ReportDataResponse(reportId: String, result: Elem)

  def props(reportActor: ActorRef) = Props(classOf[ReportDataBuilder], reportActor)
}