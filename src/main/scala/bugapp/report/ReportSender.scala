package bugapp.report

import java.io.ByteArrayInputStream
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.BugApp
import bugapp.mail.MailerActor.SendMail
import bugapp.mail.{Attachment, MailMessage}
import bugapp.report.ReportActor.{GetReport, Report, ReportResult}
import bugapp.report.ReportTypes.{SlaReport, WeeklyReport}

/**
  *
  */
class ReportSender(val reportActor: ActorRef, val mailerActor: ActorRef) extends Actor with ActorLogging {
  import ReportSender._

  private var requests = Map.empty[String, SendReport]

  override def receive: Receive = {
    case c@SendWeeklyReport(weeks, mailDetails) =>
      val endDate = BugApp.toDate
      val startDate = BugApp.fromDate(endDate, weeks)
      val request = GetReport(WeeklyReport, startDate, endDate, weeks)
      sendReportRequest(c, request)

    case c@SendSlaReport(weeks, mailDetails) =>
      val endDate = BugApp.toDate
      val startDate = BugApp.fromDate(endDate, weeks)
      val request = GetReport(SlaReport, startDate, endDate, weeks)
      sendReportRequest(c, request)

    case ReportResult(result, _) if result.isDefined =>
      val report = result.get

      requests.get(report.reportId) match {
        case Some(SendWeeklyReport(_, mailDetails)) =>
          sendWeeklyReportMail(report, mailDetails)
        case Some(SendSlaReport(_, mailDetails)) =>
          sendSlaReportMail(report, mailDetails)
        case _ =>
      }
      requests -= report.reportId

    case ReportResult(_, error) if error.isDefined =>
      val message = error.get.message
      log.error(message)
  }

  private def sendReportRequest(command: SendReport, request: GetReport) = {
    requests += (request.reportId -> command)

    reportActor ! request
    sender ! Ack(request.reportId)
  }

  private def sendWeeklyReportMail(report: Report, mailDetails: MailDetails) = {
    val is = new ByteArrayInputStream(report.data)
    val attachment = new Attachment("WeeklyReport.pdf", report.contentType, is)

    val subject = s"ProdSupport Weekly Report - ${OffsetDateTime.now.format(DateTimeFormatter.ISO_DATE)}"
    val text =
      """Hi All,
        |
        |Please find Production Support Weekly status report attached.
      """.stripMargin

    val mailMessage = message(mailDetails.from, subject, mailDetails.to.toArray, mailDetails.cc.toArray, text, attachment)

    mailerActor ! SendMail(mailMessage)
  }

  private def sendSlaReportMail(report: Report, mailDetails: MailDetails) = {
    val is = new ByteArrayInputStream(report.data)
    val attachment = new Attachment("SlaReport.pdf", report.contentType, is)

    val subject = s"ProdSupport Sla Report - ${OffsetDateTime.now.format(DateTimeFormatter.ISO_DATE)}"
    val text =
      """Hi All,
        |
        |Please find Production Support SLA status report attached.
      """.stripMargin

    val mailMessage = message(mailDetails.from, subject, mailDetails.to.toArray, mailDetails.cc.toArray, text, attachment)

    mailerActor ! SendMail(mailMessage)
  }
}

object ReportSender {
  def props(reportActor: ActorRef, mailerActor: ActorRef) = Props(classOf[ReportSender], reportActor, mailerActor)

  def message(from: String, subject: String, to: Array[String], cc: Array[String], text: String, attachment: Attachment) =
    new MailMessage(UUID.randomUUID().toString, from, subject, to, cc, Array(), null, text, null, "UTF-8", Array(attachment))

  case class MailDetails(from: String, to: Seq[String], cc: Seq[String])

  sealed trait SendReport
  case class SendWeeklyReport(weeks: Int, mailDetails: MailDetails) extends SendReport
  case class SendSlaReport(weeks: Int, mailDetails: MailDetails) extends SendReport
  case class Ack(id: String)
}
