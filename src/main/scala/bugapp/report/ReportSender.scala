package bugapp.report

import java.io.ByteArrayInputStream
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.{BugApp, EmailConfig}
import bugapp.mail.MailerActor.SendMail
import bugapp.mail.{Attachment, MailMessage}
import bugapp.report.ReportActor.{GetReport, Report, ReportResult}
import bugapp.report.ReportTypes.{ReportType, SlaReport, WeeklyReport}

/**
  *
  */
class ReportSender(val reportActor: ActorRef, val mailerActor: ActorRef) extends Actor with ActorLogging with EmailConfig {
  import ReportSender._

  private var requests = Map.empty[String, GetReport]

  override def receive: Receive = {
    case SendWeeklyReport(weeks) =>
      val endDate = BugApp.toDate
      val startDate = BugApp.fromDate(endDate, weeks)
      val request = GetReport(WeeklyReport, startDate, endDate, weeks)
      sendReportRequest(request)

    case SendSlaReport(weeks) =>
      val endDate = BugApp.toDate
      val startDate = BugApp.fromDate(endDate, weeks)
      val request = GetReport(SlaReport, startDate, endDate, weeks)
      sendReportRequest(request)

    case ReportResult(result, _) if result.isDefined =>
      val report = result.get

      requests.get(report.reportId) match {
        case Some(request) if request.reportType == WeeklyReport =>
          sendWeeklyReportMail(report)
        case Some(request) if request.reportType == SlaReport =>
          sendSlaReportMail(report)
        case _ =>
      }
      requests -= report.reportId

    case ReportResult(_, error) if error.isDefined =>
      val message = error.get.message
      log.error(message)
  }

  private def sendReportRequest(request: GetReport) = {
    requests += (request.reportId -> request)

    reportActor ! request
    sender ! Ack(request.reportId)
  }

  private def sendWeeklyReportMail(report: Report) = {
    val is = new ByteArrayInputStream(report.data)
    val attachment = new Attachment("WeeklyReport.pdf", report.contentType, is)

    val mailId = UUID.randomUUID().toString
    val subject = s"ProdSupport Weekly Report - ${OffsetDateTime.now.format(DateTimeFormatter.ISO_DATE)}"
    val text =
      """Hi All,
        |
        |Please find Production Support Weekly status report attached.
      """.stripMargin

    val mailMessage = new MailMessage(
      mailId,
      from,
      subject,
      to("weekly"),
      cc("weekly"),
      Array(),
      null,
      text,
      null,
      "UTF-8",
      Array(attachment)
    )
    mailerActor ! SendMail(mailMessage)
  }

  private def sendSlaReportMail(report: Report) = {
    val is = new ByteArrayInputStream(report.data)
    val attachment = new Attachment("SlaReport.pdf", report.contentType, is)

    val mailId = UUID.randomUUID().toString
    val subject = s"ProdSupport Sla Report - ${OffsetDateTime.now.format(DateTimeFormatter.ISO_DATE)}"
    val text =
      """Hi All,
        |
        |Please find Production Support SLA status report attached.
      """.stripMargin

    val mailMessage = new MailMessage(
      mailId,
      from,
      subject,
      to("sla"),
      cc("sla"),
      Array(),
      null,
      text,
      null,
      "UTF-8",
      Array(attachment)
    )
    mailerActor ! SendMail(mailMessage)
  }
}

object ReportSender {
  def props(reportActor: ActorRef, mailerActor: ActorRef) = Props(classOf[ReportSender], reportActor, mailerActor)

  case class SendWeeklyReport(weeks: Int)
  case class SendSlaReport(weeks: Int)
  case class Ack(id: String)
}
