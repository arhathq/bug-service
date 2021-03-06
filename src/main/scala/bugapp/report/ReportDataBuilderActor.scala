package bugapp.report

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import bugapp.BugApp
import bugapp.report.ReportActor.{ReportDataPrepared, ReportError}
import bugapp.report.ReportTypes.ReportType
import bugapp.report.ReportWorker.WorkFailed
import bugapp.report.WorkersFactory.WorkersType
import bugapp.report.model.{MapValue, ReportField, StringValue}
import bugapp.repository.Bug

import scala.collection.mutable

/**
  *
  */
class ReportDataBuilderActor(reportActor: ActorRef, reportWorkersType: WorkersType) extends Actor with ActorLogging {
  import ReportDataBuilderActor._

  private val jobs = mutable.Map.empty[String, Set[ActorRef]]
  private val requests = mutable.Map.empty[String, Map[String, Any]]
  private val data = mutable.Map.empty[String, List[model.ReportData]]

  private val reportWorkers = WorkersFactory.createWorkers(reportWorkersType, context)

  override def receive: Receive = {
    case GetReportData(reportId, reportParams, bugs) =>
      val reportType: ReportType = reportParams(ReportParams.ReportType).asInstanceOf[ReportType]
      val workers = reportWorkers.create(reportType)
      jobs += reportId -> workers
      requests += reportId -> reportParams
      log.info(s"Job [$reportId], Workers $jobs")
      workers.foreach(_ ! ReportDataRequest(reportId, reportParams, bugs))

    case ReportDataResponse(reportId, result: model.ReportData) =>
      data.get(reportId) match {
        case Some(reportDataList) => data += reportId -> (result :: reportDataList)
        case None => data += reportId -> (result :: Nil)
      }
      jobs.get(reportId) match {
        case Some(workers) =>
          killWorker(sender)
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

    case WorkFailed(reportId, message) =>
      jobs remove reportId match {
        case Some(workers) =>
          workers.foreach(killWorker)
          data -= reportId
          requests -= reportId
          reportActor ! ReportError(reportId, message)
        case None =>
      }
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    message match {
      case Some(msg) => msg match {
        case GetReportData(reportId, _, _) =>
          jobs -= reportId
          requests -= reportId
          reportActor ! ReportError(reportId, reason.getMessage)
      }

      case None =>
    }
  }

  def buildReportData(reportId: String): ReportDataPrepared = {
    val reportType = requests(reportId)(ReportParams.ReportType).asInstanceOf[ReportType]
    val excludedComponents = requests(reportId)(ReportParams.ExcludedComponents).asInstanceOf[Seq[String]]

    val dataList = data(reportId)

    val reportData = dataList.map(data => ReportField(data.name, data.fields))
    data -= reportId

    val result = report(BugApp.toDate, createNote(excludedComponents), reportData)

    ReportDataPrepared(reportId, reportType, result)
  }

  def createNote(excludedComponents: Seq[String]): String = {
    if (excludedComponents.nonEmpty)
      s"* Bugs ${excludedComponents.mkString("\"", "\", \"", "\"")} excluded from report"
    else
      ""
  }
}

object ReportDataBuilderActor {
  case class GetReportData(reportId: String, reportParams: Map[String, Any], bugs: Seq[Bug])

  case class ReportDataRequest(reportId: String, reportParams: Map[String, Any], bugs: Seq[Bug])
  case class ReportDataResponse[T](reportId: String, result: T)

  def props(reportActor: ActorRef, reportWorkersType: WorkersType) = Props(classOf[ReportDataBuilderActor], reportActor, reportWorkersType)

  def report(date: OffsetDateTime, notes: String, reportData: List[ReportField]): model.ReportData = {
    model.ReportData("bug-reports",
      MapValue(
        Seq(
          ReportField("report-header",
            MapValue(
              ReportField("date", StringValue(date.toString))
            )
          ),
          ReportField("report-footer",
            MapValue(
              ReportField("note", StringValue(notes))
            )
          )
        ) ++ reportData: _*
      )
    )
  }

  def killWorker(worker: ActorRef): Unit = worker ! PoisonPill
}