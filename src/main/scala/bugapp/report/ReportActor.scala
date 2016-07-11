package bugapp.report

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.ReportConfig
import bugapp.report.ReportProtocol.{Bugs, GenerateReport, ReportError, ReportGenerated}
import bugapp.repository.BugRepository

/**
  * @author Alexander Kuleshov
  */
class ReportActor(bugRepository: BugRepository) extends Actor with ActorLogging with ReportConfig {

  var jobs: Map[String, Map[String, ActorRef]] = Map()
  var senders: Map[String, ActorRef] = Map()

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {

    log.info(s"Actor created")
    super.preStart()
  }

  override def receive: Receive = {
    case GenerateReport(weeks) => {
      log.info(s"Weeks period: $weeks")
      if (jobs.size >= maxJobs)
        sender ! ReportError(s"Max reports is limited: $maxJobs")
      else {
        val jobId = reportId
        val workers = reports()
        jobs += (jobId -> workers)
        senders += (jobId -> sender)
        log.info(s"Job [$jobId], Workers $jobs")
        val bugs = bugRepository.getBugs()

        workers.foreach(x => x._2 ! Bugs(jobId, bugs))
      }
    }
    case ReportGenerated(jobId) => {
      jobs -= jobId
      log.info(s"Job [$jobId] completed, Workers $jobs")
      val client = senders get jobId
      senders -= jobId
      client.get ! s"{report: 'OK'}"
    }
  }

  def reports(): Map[String, ActorRef] = {
    Map("allOpenBugsReportActor" -> context.actorOf(Props[AllOpenBugsReportActor]))
  }

  def reportId:String = UUID.randomUUID().toString
}
