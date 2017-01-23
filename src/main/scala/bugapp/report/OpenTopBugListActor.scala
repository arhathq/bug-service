package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.report.ReportActor.dateTimeFormat
import bugapp.report.model.{IntValue, MapValue, ReportField, StringValue}
import bugapp.repository.Bug


/**
  * @author Alexander Kuleshov
  */
class OpenTopBugListActor (owner: ActorRef) extends Actor with ActorLogging {

  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, _, bugs) =>
      val prioritizedOpenBugs = bugs.filter(bug => bug.stats.status == Metrics.OpenStatus).groupBy(bug => bug.priority)
      val p1OpenBugs = prioritizedOpenBugs.getOrElse(Metrics.P1Priority, Seq())
      val p2OpenBugs = prioritizedOpenBugs.getOrElse(Metrics.P2Priority, Seq())

      val openBugsData = p1OpenBugs.map(bugData) ++ p2OpenBugs.map(bugData)

      val data = model.ReportData("open-bugs", MapValue(openBugsData: _*))

      owner ! ReportDataResponse(reportId, data)
  }

  def bugData(bug: Bug): ReportField = {
    ReportField("bug",
      MapValue(
        ReportField("id", IntValue(bug.id)),
        ReportField("priority", StringValue(bug.priority)),
        ReportField("opened", StringValue(dateTimeFormat.format(bug.opened))),
        ReportField("summary", StringValue(bug.summary)),
        ReportField("client", StringValue(bug.hardware)),
        ReportField("product", StringValue(bug.product))
      )
    )
  }

}

object OpenTopBugListActor {
  def props(owner: ActorRef) = Props(classOf[OpenTopBugListActor], owner)
}