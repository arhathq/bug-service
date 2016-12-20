package bugapp.report

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import bugapp.{BugApp, ReportConfig}
import bugapp.bugzilla.RepositoryEventBus
import bugapp.report.ReportDataBuilder.{GetReportData, ReportData}
import bugapp.report.ReportGenerator.{GenerateReport, ReportGenerated}
import bugapp.repository.BugRepository

import scala.collection.mutable

/**
  * @author Alexander Kuleshov
  */
class ReportActor(bugRepository: BugRepository, repositoryEventBus: RepositoryEventBus, excludedComponents: Seq[String]) extends Actor with ActorLogging with ReportConfig {
  import bugapp.report.ReportActor._

  private implicit val ec = context.dispatcher

  private val senders = mutable.Map.empty[String, ActorRef]

  private val pendingRequests = mutable.Set.empty[(GetReport, ActorRef)]

  private val reportDataBuilders = mutable.Map.empty[String, ActorRef]
  private val reportBuilder = context.actorOf(ReportGenerator.props(fopConf, self), "reportGenerator")

  override def receive: Receive = reportManagement

  def reportManagement: Receive = {
    case GetReport(reportType, startDate, endDate, weekPeriod) =>
      log.info(s"Period: [$startDate - $endDate]")
      if (senders.size >= maxJobs)
        sender !  ReportResult(report = None, error = Some(s"Max reports is limited: $maxJobs"))
      else {
        val reportId = newReportId
        senders += reportId -> sender
        val bugsFuture = bugRepository.getBugs

        bugsFuture.foreach { bugs =>
          val reportDataBuilder = context.actorOf(ReportDataBuilder.props(self))
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
        }
      }

    case ReportData(reportId, reportType, result) =>
      val template = reportTemplate(reportType)
      reportDataBuilders.remove(reportId).foreach { reportDataBuilder =>
        reportDataBuilder ! PoisonPill
      }
      reportBuilder ! GenerateReport(reportId, template, result)

    case ReportGenerated(report) =>
      senders.remove(report.reportId) match {
        case Some(sender) => sender ! ReportResult(Some(report.data))
        case None =>
      }

    case ReportError(reportId, message) =>
      senders.remove(reportId) match {
        case Some(sender) => sender ! ReportResult(report = None, error = Some(message))
        case None =>
      }

    case RepositoryEventBus.UpdateRequired =>
      log.debug("Switching to System Management Mode")
      context.become(systemManagement)
      repositoryEventBus.publish(RepositoryEventBus.UpdateGrantedEvent())

  }

  def systemManagement: Receive = { //TODO: update duplicated code

    case request: GetReport => pendingRequests.add((request, sender))

    case ReportData(reportId, reportType, result) =>
      val template = reportTemplate(reportType)
      reportDataBuilders.remove(reportId).foreach { reportDataBuilder =>
        reportDataBuilder ! PoisonPill
      }
      reportBuilder ! GenerateReport(reportId, template, result)

    case ReportGenerated(report) =>
      senders.remove(report.reportId) match {
        case Some(sender) => sender ! ReportResult(Some(report.data))
        case None =>
      }

    case ReportError(reportId, message) =>
      senders.remove(reportId) match {
        case Some(sender) => sender ! ReportResult(report = None, error = Some(message))
        case None =>
      }

    case RepositoryEventBus.UpdateCompleted =>
      log.debug("Switching to Report Mode")
      context.become(reportManagement)
      log.debug(s"Number of pending requests before resend: ${pendingRequests.size}")
      pendingRequests.foreach(req => req._2 ! req._1)
      pendingRequests.clear()
      log.debug(s"Number of pending requests after resend: ${pendingRequests.size}")
  }

  def newReportId: String = UUID.randomUUID().toString
}

object ReportActor {
  private[report] val dateFormat = DateTimeFormatter.ISO_ZONED_DATE_TIME

  def props(bugRepository: BugRepository, repositoryEventBus: RepositoryEventBus, excludedComponents: Seq[String]) =
    Props(classOf[ReportActor], bugRepository, repositoryEventBus, excludedComponents)

  def formatNumber(number: Int): String = if (number == 0) "" else number.toString

  case class GetReport(reportType: String, startDate: OffsetDateTime, endDate: OffsetDateTime, weekPeriod: Int)
  case class ReportResult(report: Option[Array[Byte]], error: Option[String] = None)

  case class ReportError(reportId: String, message: String)
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