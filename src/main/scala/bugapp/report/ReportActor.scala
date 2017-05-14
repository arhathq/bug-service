package bugapp.report

import java.time.{LocalDate, OffsetDateTime}
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import bugapp.{BugApp, ReportConfig}
import bugapp.bugzilla.RepositoryEventBus
import bugapp.report.ReportDataBuilderActor.GetReportData
import bugapp.report.ReportGenerator.GenerateReport
import bugapp.report.ReportTypes.ReportType
import bugapp.report.converter.XmlReportDataConverter
import bugapp.repository.BugRepository

import scala.collection.mutable
import scala.xml.{Elem, Node, Null, TopScope}

/**
  * @author Alexander Kuleshov
  */
class ReportActor(bugRepository: BugRepository, repositoryEventBus: RepositoryEventBus, excludedComponents: Seq[String]) extends Actor with ActorLogging with ReportConfig {
  import bugapp.report.ReportActor._

  private implicit val ec = context.dispatcher

  private val senders = mutable.Map.empty[String, ActorRef]

  private val pendingRequests = mutable.Set.empty[(GetReport, ActorRef)]

  private val reportDataBuilders = mutable.Map.empty[String, ActorRef]
  private val reportDataConverter = new XmlReportDataConverter()
  private val reportBuilder = context.actorOf(ReportGenerator.props(fopConf, reportDir, self), "reportGenerator")

  override def receive: Receive = reportManagement

  def reportManagement: Receive = {
    case request@GetReport(reportType, startDate, endDate, weekPeriod) =>
      log.info(s"Period: [$startDate - $endDate]")
      if (senders.size >= maxJobs)
        sender !  ReportResult(report = None, error = Some(ReportError("", s"Max reports is limited: $maxJobs")))
      else {
        val reportId = request.reportId
        senders += reportId -> sender
        bugRepository.getBugs.map { bugs =>
          val reportDataBuilder = context.actorOf(ReportDataBuilderActor.props(self, WorkersFactory.Report))
          reportDataBuilders += (reportId -> reportDataBuilder)
          val reportParams = Map[String, Any](
            ReportParams.ReportType -> reportType,
            ReportParams.StartDate -> startDate,
            ReportParams.EndDate -> endDate,
            ReportParams.BugtrackerUri -> BugApp.bugzillaUrl,
            ReportParams.WeekPeriod -> weekPeriod,
            ReportParams.ExcludedComponents -> excludedComponents
          )
          reportDataBuilder ! GetReportData(reportId, reportParams, bugs)
        }.recover {
          case t: Throwable =>
            senders.remove(reportId) match {
              case Some(sender) =>
                sender ! ReportResult(report = None, error = Some(ReportError(reportId, t.getMessage)))
              case None =>
            }
        }
      }

    case evt: ReportEvent => handleReportEvent(evt)

    case RepositoryEventBus.UpdateRequired =>
      log.debug("Switching to System Management Mode")
      context.become(systemManagement)
      repositoryEventBus.publish(RepositoryEventBus.UpdateGrantedEvent())

  }

  def systemManagement: Receive = {

    case request: GetReport => pendingRequests.add((request, sender))

    case evt: ReportEvent => handleReportEvent(evt)

    case RepositoryEventBus.UpdateCompleted =>
      log.debug("Switching to Report Mode")
      context.become(reportManagement)
      log.debug(s"Number of pending requests before resend: ${pendingRequests.size}")
      pendingRequests.foreach(req => req._2 ! req._1)
      pendingRequests.clear()
      log.debug(s"Number of pending requests after resend: ${pendingRequests.size}")
  }

  private def handleReportEvent(event: ReportEvent) = event match {
    case ReportDataPrepared(reportId, reportType, result) =>
      try {
        val template = reportTemplate(reportType)
        reportDataBuilders.remove(reportId).foreach { reportDataBuilder =>
          reportDataBuilder ! PoisonPill
        }
        val reportName = report(reportDir, reportType)
        reportBuilder ! GenerateReport(reportId, reportName, template, reportDataConverter.convert(result))
      } catch {
        case t: Throwable => self ! ReportError(reportId, t.getMessage)
      }

    case ReportGenerated(report) =>
      senders.remove(report.reportId) match {
        case Some(sender) => sender ! ReportResult(Some(report))
        case None =>
      }

    case error @ ReportError(reportId, _) =>
      senders.remove(reportId) match {
        case Some(sender) => sender ! ReportResult(report = None, error = Some(error))
        case None =>
      }

  }

}

object ReportActor {
  private[report] val dateTimeFormat = DateTimeFormatter.ISO_ZONED_DATE_TIME
  private[report] val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE
  private val reportDateFormat = DateTimeFormatter.ofPattern("uuuuMMdd")

  def props(bugRepository: BugRepository, repositoryEventBus: RepositoryEventBus, excludedComponents: Seq[String]) =
    Props(classOf[ReportActor], bugRepository, repositoryEventBus, excludedComponents)

  def formatNumber(number: Int): String = if (number == 0) "" else number.toString
  def createXmlElement(name: String, child: Node*): Elem = Elem.apply(null, name, Null, TopScope, true, child: _*)

  def report(dir: String, reportType: ReportType): String =
    s"$dir/${reportType.name}${reportDateFormat.format(LocalDate.now)}.pdf"

  case class GetReport(reportType: ReportType, startDate: OffsetDateTime, endDate: OffsetDateTime, weekPeriod: Int) {
    val reportId: String = UUID.randomUUID().toString
  }
  case class ReportResult(report: Option[Report], error: Option[ReportError] = None)

  sealed trait ReportEvent
  case class ReportDataPrepared(reportId: String, reportType: ReportType, result: model.ReportData) extends ReportEvent
  case class ReportGenerated(report: Report) extends ReportEvent
  case class ReportError(reportId: String, message: String) extends ReportEvent

  case class Report(reportId: String, name: String, contentType: String, data: Array[Byte])
}

object ReportParams {
  val ReportType = "reportType"
  val ReportTemplate = "reportTemplate"
  val StartDate = "startDate"
  val EndDate = "endDate"
  val BugtrackerUri = "bugtrackerUri"
  val WeekPeriod = "weekPeriod"
  val ExcludedComponents = "excludedComponents"
}

object ReportTypes {
  sealed trait ReportType {
    def name: String
  }
  case object WeeklyReport extends ReportType {
    override def name = "weekly"
  }
  case object SlaReport extends ReportType {
    override def name = "sla"
  }

  def from(name: String): Either[String, ReportType] = name match {
    case "weekly" => Right(WeeklyReport)
    case "sla" => Right(SlaReport)
    case _ => Left(s"Invalid report name. Available reports $reportNames")
  }

  def reportNames = List(WeeklyReport.name, SlaReport.name)
}