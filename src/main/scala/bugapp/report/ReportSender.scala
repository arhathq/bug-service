package bugapp.report

import java.io.ByteArrayInputStream
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.{BugApp, EmailConfig}
import bugapp.mail.MailerActor.SendMail
import bugapp.mail.{Attachment, MailMessage}
import bugapp.report.ReportActor.{GetReport, ReportResult}
import bugapp.report.ReportTypes.WeeklyReport

/**
  *
  */
class ReportSender(val reportActor: ActorRef, val mailerActor: ActorRef) extends Actor with ActorLogging with EmailConfig {
  import ReportSender._

  override def receive: Receive = {
    case SendWeeklyReport(weeks) =>
      val endDate = BugApp.toDate
      val startDate = BugApp.fromDate(endDate, weeks)

      val request = GetReport(WeeklyReport, startDate, endDate, weeks)

      reportActor ! request
      sender ! Ack(request.reportId)

    case ReportResult(result, _) if result.isDefined =>
      val report = result.get

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

    case ReportResult(_, error) if error.isDefined =>
      val message = error.get.message
      log.error(message)
  }

}

object ReportSender {
  def props(reportActor: ActorRef, mailerActor: ActorRef) = Props(classOf[ReportSender], reportActor, mailerActor)

  case class SendWeeklyReport(weeks: Int)
  case class Ack(id: String)
}
