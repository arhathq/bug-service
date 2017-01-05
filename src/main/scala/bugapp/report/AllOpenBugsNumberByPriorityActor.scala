package bugapp.report

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.repository.Bug
import org.jfree.data.category.DefaultCategoryDataset

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
      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]

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
            <image>
              <content-type>image/jpeg</content-type>
              <content-value>{allOpenBugsChart(endDate, p1OpenBugs ++ p2OpenBugs)}</content-value>
            </image>
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

  def allOpenBugsChart(endDate: OffsetDateTime, bugs: Seq[Bug]): String = {
    val marks = bugs.groupBy(bug => bug.stats.openMonth).filter(tuple => tuple._2.nonEmpty).keys.toSeq.sortWith((v1, v2) => v1 < v2)
    val dataSet = new DefaultCategoryDataset()
    allOpenBugsChartData(Metrics.P1Priority, marks, bugs.filter(bug => bug.priority == Metrics.P1Priority)).foreach(data => dataSet.addValue(data._1, data._2, data._3))
    allOpenBugsChartData(Metrics.P2Priority, marks, bugs.filter(bug => bug.priority == Metrics.P2Priority)).foreach(data => dataSet.addValue(data._1, data._2, data._3))
    ChartGenerator.generateBase64OpenHighPriorityBugs(dataSet)
  }

  def allOpenBugsChartData(priority: String, marks:Seq[String], bugs: Seq[Bug]): Seq[(Int, String, String)] = {
    val grouped = bugs.groupBy(bug => bug.stats.openMonth)
    marks.map { mark =>
      grouped.get(mark) match {
        case Some(v) => (v.length, priority, mark)
        case None => (0, priority, mark)
      }
    }
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