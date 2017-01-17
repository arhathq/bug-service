package bugapp.report

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.repository.Bug
import org.jfree.data.category.DefaultCategoryDataset

/**
  *
  */
class AllOpenBugsNumberByPriorityChartActor(owner: ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>

      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]

      val openBugs = bugs.filter(bug => bug.stats.status == Metrics.OpenStatus)
      val prioritizedOpenBugs = openBugs.groupBy(bug => bug.priority)
      val p1OpenBugs = prioritizedOpenBugs.getOrElse(Metrics.P1Priority, Seq())
      val p2OpenBugs = prioritizedOpenBugs.getOrElse(Metrics.P2Priority, Seq())

      val data =
        <all-open-bugs-chart>
          <image>
            <content-type>image/jpeg</content-type>
            <content-value>{allOpenBugsChart(endDate, p1OpenBugs ++ p2OpenBugs)}</content-value>
          </image>
        </all-open-bugs-chart>

      owner ! ReportDataResponse(reportId, data)
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

object AllOpenBugsNumberByPriorityChartActor {
  def props(owner: ActorRef) = Props(classOf[AllOpenBugsNumberByPriorityChartActor], owner)
}