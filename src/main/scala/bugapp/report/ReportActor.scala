package bugapp.report

import java.time.LocalDate
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import bugapp.ReportConfig
import bugapp.report.ReportDataBuilder.{GetReportData, ReportDataResponse}
import bugapp.report.ReportGenerator.{GenerateReport, ReportGenerated}
import bugapp.repository.BugRepository

import scala.collection.mutable

/**
  * @author Alexander Kuleshov
  */
class ReportActor(bugRepository: BugRepository) extends Actor with ActorLogging with ReportConfig {
  import bugapp.report.ReportActor._

  implicit val ec = context.dispatcher

  private val senders = mutable.Map.empty[String, ActorRef]

  val reportBuilder = context.actorOf(ReportGenerator.props(fopConf, templateDir, self), "reportGenerator")

  override def receive: Receive = {
    case GetReport(reportType, weeks) =>
      log.info(s"Weeks period: $weeks")
      if (senders.size >= maxJobs)
        sender !  ReportResult(report = None, error = Some(s"Max reports is limited: $maxJobs"))
      else {
        val reportId = newReportId
        senders += reportId -> sender
        val bugsFuture = bugRepository.getBugs(LocalDate.now.minusWeeks(weeks))

        bugsFuture.foreach { bugs =>
          val reportDataBuilder = context.actorOf(ReportDataBuilder.props(self))
          reportDataBuilder ! GetReportData(reportId, reportType, bugs)
        }
      }

    case ReportDataResponse(reportId, data) =>
      sender ! PoisonPill
      reportBuilder ! GenerateReport(reportId, data)

    case ReportGenerated(reportId, report) =>
      senders.remove(reportId) match {
        case Some(sender) => sender ! ReportResult(Some(report))
        case None =>
      }

    case ReportError(reportId, message) =>
      senders.remove(reportId) match {
        case Some(sender) => sender ! ReportResult(report = None, error = Some(message))
        case None =>
      }

  }

  def newReportId: String = UUID.randomUUID().toString
}

object ReportActor {
  def props(bugRepository: BugRepository) = Props(classOf[ReportActor], bugRepository)
  case class GetReport(reportType: String, weeks: Int)
  case class ReportResult(report: Option[Array[Byte]], error: Option[String] = None)

  case class ReportError(reportId: String, message: String)
}