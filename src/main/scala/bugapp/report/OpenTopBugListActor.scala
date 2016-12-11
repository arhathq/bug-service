package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.report.ReportActor.dateFormat
import bugapp.repository.Bug

import scala.xml.Elem

/**
  * @author Alexander Kuleshov
  */
class OpenTopBugListActor (owner: ActorRef) extends Actor with ActorLogging {

  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val prioritizedOpenBugs = bugs.filter(bug => bug.stats.status == Metrics.OpenStatus).groupBy(bug => bug.priority)
      val p1OpenBugs = prioritizedOpenBugs.getOrElse(Metrics.P1Priority, Seq())
      val p2OpenBugs = prioritizedOpenBugs.getOrElse(Metrics.P2Priority, Seq())

      val data =
        <open-bugs>
          {p1OpenBugs.map(bugElem)}
          {p2OpenBugs.map(bugElem)}
        </open-bugs>

      owner ! ReportDataResponse(reportId, data)
  }

  def bugElem(bug: Bug): Elem = {
    <bug>
      <id>{bug.id}</id>
      <priority>{bug.priority}</priority>
      <opened>{dateFormat.format(bug.opened)}</opened>
      <summary>{bug.summary}</summary>
      <client>{bug.hardware}</client>
      <product>{bug.product}</product>
    </bug>
  }

}

object OpenTopBugListActor {
  def props(owner: ActorRef) = Props(classOf[OpenTopBugListActor], owner)
}