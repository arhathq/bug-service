package bugapp.report

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.collection.mutable
import scala.xml.Elem

/**
  * Created by arhathq on 04.08.2016.
  */
class ReportDataBuilder(reportActor: ActorRef) extends Actor with ActorLogging {

  import bugapp.report.ReportProtocol._

  private val jobs = mutable.Map.empty[String, Set[ActorRef]]
  private val data = mutable.Map.empty[String, List[Elem]]

  def createWorkers(reportId: String): Set[ActorRef] = {
    Set(context.actorOf(Props[AllOpenBugsReportActor]))
  }

  override def receive: Receive = {
    case PrepareReportData(reportId, bugs) =>
      val workers = createWorkers(reportId)
      jobs += reportId -> workers
      log.info(s"Job [$reportId], Workers $jobs")
      workers.foreach(_ ! PrepareReportData(reportId, bugs))

    case ReportData(reportId, reportData) =>
      data.get(reportId) match {
        case Some(reportDataList) => data += reportId -> (reportData :: reportDataList)
      }
      jobs.get(reportId) match {
        case Some(workers) =>
          workers -= sender
          if (workers.isEmpty) {
            jobs -= reportId
            reportActor ! ReportDataPrepared(reportId, buildReportData(reportId))
          } else
            jobs += reportId -> workers
      }
  }

  def buildReportData(reportId: String): Elem = {
    val xml3 = new xml.NodeBuffer()
    data.get(reportId) match {
      case Some(dataList) =>
        dataList.foreach(xml3.append)
        data -= reportId
    }

    val dataXml =
      <bug-reports>
        <date>${OffsetDateTime.now}</date>
        {xml3}
      </bug-reports>
    dataXml
  }
}

object ReportDataBuilder {
  def props(reportActor: ActorRef) = Props(classOf[ReportDataBuilder], reportActor)
}
