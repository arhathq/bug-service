package bugapp.report

import java.time.LocalDate
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.ReportConfig
import bugapp.repository.BugRepository

import scala.collection.mutable

/**
  * @author Alexander Kuleshov
  */
class ReportActor(bugRepository: BugRepository) extends Actor with ActorLogging with ReportConfig {

  import bugapp.report.ReportProtocol._

  private val senders = mutable.Map.empty[String, ActorRef]

  val reportDataBuilder = context.actorOf(ReportDataBuilder.props(self))
  val reportBuilder = context.actorOf(ReportGenerator.props(fopConf, templateDir, self))

  override def receive: Receive = {
    case GenerateReport(weeks) =>
      log.info(s"Weeks period: $weeks")
      if (senders.size >= maxJobs)
        sender ! ReportFailed(s"Max reports is limited: $maxJobs")
      else {
        val reportId = newReportId
        senders += reportId -> sender
        reportDataBuilder ! PrepareReportData(reportId, bugRepository.getBugs(LocalDate.now.minusWeeks(weeks)))
      }

    case ReportDataPrepared(reportId, data) =>
      reportBuilder ! PrepareReport(reportId, data)

    case ReportPrepared(reportId, report) =>
      senders.remove(reportId) match {
        case Some(sender) => sender ! ReportGenerated(report)
      }
  }

  def newReportId: String = UUID.randomUUID().toString
}

object ReportActor {
  def props(bugRepository: BugRepository) = Props(classOf[ReportActor], bugRepository)
}