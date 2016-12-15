package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.repository.Bug

import scala.xml._

/**
  * @author Alexander Kuleshov
  */
class AllOpenBugsNumberByPriorityActor(owner: ActorRef) extends Actor with ActorLogging {
  import AllOpenBugsNumberByPriorityActor._

  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val excludedComponents: Seq[String] = reportParams(ReportParams.ExcludedComponents).asInstanceOf[Seq[String]]

      val openBugs = bugs.filter(bug => bug.stats.status == Metrics.OpenStatus)
      val prioritizedOpenBugs = openBugs.groupBy(bug => bug.priority)
      val p1OpenBugs = prioritizedOpenBugs.getOrElse(Metrics.P1Priority, Seq())
      val p2OpenBugs = prioritizedOpenBugs.getOrElse(Metrics.P2Priority, Seq())
      val p3OpenBugs = prioritizedOpenBugs.getOrElse(Metrics.P3Priority, Seq())
      val p4OpenBugs = prioritizedOpenBugs.getOrElse(Metrics.P4Priority, Seq())
      val npOpenBugs = prioritizedOpenBugs.getOrElse(Metrics.NPPriority, Seq())

      val data =
          <all-open-bugs>
            { prioritizedBugsElem(Metrics.P1Priority, p1OpenBugs) }
            { prioritizedBugsElem(Metrics.P2Priority, p2OpenBugs) }
            { prioritizedBugsElem(Metrics.P3Priority, p3OpenBugs) }
            { prioritizedBugsElem(Metrics.P4Priority, p4OpenBugs) }
            { prioritizedBugsElem(Metrics.NPPriority, npOpenBugs) }
            {prioritizedBugsElem("Grand Total", openBugs)}
            <excludedComponents>{excludedComponents.mkString("\'", "\', \'", "\'")}</excludedComponents>
          </all-open-bugs>

       owner ! ReportDataResponse(reportId, data)
  }

  def prioritizedBugsElem(priority: String, bugs: Seq[Bug]): Elem = {
    val data = splitBugsByOpenPeriod(bugs).map(tuple => tuple._1 -> tuple._2.length)

    <prioritized-bugs>
      <priority>{priority}</priority>
      {data.map(tuple => Elem.apply(null, s"${tuple._1}", Null, TopScope, true, Text(tuple._2.toString)))}
      <total>{val total = data.map(_._2).sum; if(total > 0) total}</total>
    </prioritized-bugs>
  }
}

object AllOpenBugsNumberByPriorityActor {
  def props(owner: ActorRef) = Props(classOf[AllOpenBugsNumberByPriorityActor], owner)

  def splitBugsByOpenPeriod(bugs: Seq[Bug]): Seq[(String, Seq[Bug])] = {
    bugs.groupBy { bug =>
      val daysOpen = Metrics.durationInBusinessDays(bug.stats.openTime, bug.stats.resolvedTime)

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