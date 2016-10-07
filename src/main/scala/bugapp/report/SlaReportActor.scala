package bugapp.report

import java.time.OffsetDateTime

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.report.ReportDataBuilder.ReportDataRequest
import bugapp.repository.Bug

/**
  *
  */
class SlaReportActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case ReportDataRequest(reportId, bugs) =>
      bugs.filter(SlaReportActor.bugsFor4Weeks)
  }

}

object SlaReportActor {
  def props() = Props(classOf[SlaReportActor])

  val bugsFor4Weeks: (Bug) => Boolean = bug => {
    val now = OffsetDateTime.now
//    if (now.minus(bug.opened))
    true
  }
}