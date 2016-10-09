package bugapp.report

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.repository.Bug

/**
  *
  */
class SlaReportActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case ReportDataRequest(reportId, bugs) =>
      val bugs4weeks = bugs.filter(SlaReportActor.bugsFor4Weeks)
//      SlaReportActor.slaBugs(bugs4weeks)
      val outSlaBugs = SlaReportActor.bugsOutSla(bugs4weeks)
      val p1 = outSlaBugs.getOrElse("P1", Seq()).length
      val p2 = outSlaBugs.getOrElse("P2", Seq()).length

      val data =
        <out-sla-bugs>
          <number-of-bugs>
            <priority>P1</priority>
            <number>{p1}</number>
          </number-of-bugs>
          <number-of-bugs>
            <priority>P2</priority>
            <number>{p2}</number>
          </number-of-bugs>
          <number-of-bugs>
            <priority>Grand Total</priority>
            <number>{p1 + p2}</number>
          </number-of-bugs>
        </out-sla-bugs>

      context.parent ! ReportDataResponse(reportId, data)
  }
}

object SlaReportActor {
  def props() = Props(classOf[SlaReportActor])

  val bugsFor4Weeks: (Bug) => Boolean = bug => {
    val from = OffsetDateTime.now.minusWeeks(4)
    bug.priority match {
      case "P1" if bug.opened.isAfter(from) => true
      case "P2" if bug.opened.isAfter(from) => true
      case _ => false
    }
  }

  val slaBugs: (Seq[Bug]) => Map[String, Seq[Bug]] = bugs => {
    bugs.groupBy(bug => bug.stats match {
      case Some(stats) if stats.resolvedPeriod == Metrics.ResolvedIn2Days => Metrics.ResolvedIn2Days
      case Some(stats) if stats.resolvedPeriod == Metrics.ResolvedIn6Days => Metrics.ResolvedIn6Days
      case Some(stats) if stats.resolvedPeriod == Metrics.ResolvedIn30Days => Metrics.ResolvedIn30Days
      case Some(stats) if stats.resolvedPeriod == Metrics.ResolvedIn90Days => Metrics.ResolvedIn90Days
    })
  }

  val bugsOutSla: (Seq[Bug]) => Map[String, Seq[Bug]] = bugs => {
    bugs.groupBy(bug => bug.stats match {
      case Some(stats) if !stats.passSla && bug.priority == "P1" => "P1"
      case Some(stats) if !stats.passSla && bug.priority == "P2" => "P2"
      case _ => "sla"
    })
  }
}