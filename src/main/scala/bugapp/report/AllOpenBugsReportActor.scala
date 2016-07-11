package bugapp.report

import akka.actor.{Actor, ActorLogging}
import bugapp.report.ReportProtocol.{Bugs, ReportGenerated}

/**
  * @author Alexander Kuleshov
  */
class AllOpenBugsReportActor extends Actor with ActorLogging {

  implicit val execution = context.dispatcher

  def receive: Receive = {
    case Bugs(jobId, futureBugs) => {
      val supervisor = sender
      log.info(s"Report request from: $supervisor")
      for {
        bugs <- futureBugs
      } yield {
        log.info(s"Bugs to process: ${bugs.size}")
        log.info(s"Complete response to: $supervisor")
        supervisor ! ReportGenerated(jobId)
      }
    }
  }
}
