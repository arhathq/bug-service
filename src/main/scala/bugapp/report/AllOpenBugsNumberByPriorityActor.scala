package bugapp.report

import akka.actor.{ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.report.model._
import bugapp.report.ReportActor.formatNumber
import bugapp.repository.Bug


/**
  * @author Alexander Kuleshov
  */
class AllOpenBugsNumberByPriorityActor(owner: ActorRef) extends ReportWorker(owner) with ActorLogging {
  import AllOpenBugsNumberByPriorityActor._

  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val excludedComponents: Seq[String] = reportParams(ReportParams.ExcludedComponents).asInstanceOf[Seq[String]]

      val openBugs = bugs.filter(bug => bug.actualStatus == Metrics.OpenStatus)
      val prioritizedOpenBugs = openBugs.groupBy(bug => bug.priority)
      val p1OpenBugs = prioritizedOpenBugs.getOrElse(Metrics.P1Priority, Seq())
      val p2OpenBugs = prioritizedOpenBugs.getOrElse(Metrics.P2Priority, Seq())
      val p3OpenBugs = prioritizedOpenBugs.getOrElse(Metrics.P3Priority, Seq())
      val p4OpenBugs = prioritizedOpenBugs.getOrElse(Metrics.P4Priority, Seq())
      val npOpenBugs = prioritizedOpenBugs.getOrElse(Metrics.NPPriority, Seq())

      val allOpenBugsData = Seq(
        prioritizedBugsData(Metrics.NPPriority, npOpenBugs),
        prioritizedBugsData(Metrics.P1Priority, p1OpenBugs),
        prioritizedBugsData(Metrics.P2Priority, p2OpenBugs),
        prioritizedBugsData(Metrics.P3Priority, p3OpenBugs),
        prioritizedBugsData(Metrics.P4Priority, p4OpenBugs),
        prioritizedBugsData("Grand Total", openBugs)
      )

      val data = model.ReportData("all-open-bugs",
        MapValue(
          ReportField("prioritized-bugs", ListValue(allOpenBugsData: _*)),
          ReportField("excludedComponents", StringValue(excludedComponents.mkString("\'", "\', \'", "\'")))
        )
      )

      owner ! ReportDataResponse(reportId, data)
  }

  def prioritizedBugsData(priority: String, bugs: Seq[Bug]): MapValue = {
    val data = splitBugsByOpenPeriod(bugs).map(tuple => tuple._1 -> tuple._2.length)

    val prioritizedBugsValue =
      ReportField("priority", StringValue(priority)) +:
        data.map(tuple => ReportField(s"${tuple._1}", StringValue(tuple._2.toString))) :+
        ReportField("total", StringValue(formatNumber(data.map(_._2).sum)))

    MapValue(prioritizedBugsValue: _*)
  }

}

object AllOpenBugsNumberByPriorityActor {
  def props(owner: ActorRef) = Props(classOf[AllOpenBugsNumberByPriorityActor], owner)

  def splitBugsByOpenPeriod(bugs: Seq[Bug]): Seq[(String, Seq[Bug])] = {
    bugs.groupBy { bug =>
      val daysOpen = Metrics.durationInBusinessDays(bug.opened, bug.resolvedTime)

      var resolvedPeriod = "period6"
      if (daysOpen < 3) resolvedPeriod = "period1"
      else if (daysOpen < 7)  resolvedPeriod = "period2"
      else if (daysOpen < 31) resolvedPeriod = "period3"
      else if (daysOpen < 91) resolvedPeriod = "period4"
      else if (daysOpen < 365) resolvedPeriod = "period5"
      resolvedPeriod
    }.toSeq
  }
}