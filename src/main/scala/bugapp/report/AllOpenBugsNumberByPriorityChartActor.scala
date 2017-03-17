package bugapp.report

import java.time.OffsetDateTime

import akka.actor.{ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.report.model.{MapValue, ReportField, StringValue}
import bugapp.repository.Bug
import org.jfree.data.category.DefaultCategoryDataset

/**
  *
  */
class AllOpenBugsNumberByPriorityChartActor(owner: ActorRef) extends ReportWorker(owner) with ActorLogging {
  override def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>

      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]

      val openBugs = bugs.filter(bug => bug.stats.status == Metrics.OpenStatus)
      val prioritizedOpenBugs = openBugs.groupBy(bug => bug.priority)
      val p1OpenBugs = prioritizedOpenBugs.getOrElse(Metrics.P1Priority, Seq())
      val p2OpenBugs = prioritizedOpenBugs.getOrElse(Metrics.P2Priority, Seq())

      val data = model.ReportData("all-open-bugs-chart", MapValue(allOpenBugsChart(endDate, p1OpenBugs ++ p2OpenBugs)))

      owner ! ReportDataResponse(reportId, data)
  }

  def allOpenBugsChart(endDate: OffsetDateTime, bugs: Seq[Bug]): ReportField = {
    val marks = bugs.groupBy(bug => bug.stats.openMonth).filter(tuple => tuple._2.nonEmpty).keys.toSeq.sortWith((v1, v2) => v1 < v2)
    val dataSet = new DefaultCategoryDataset()
    allOpenBugsChartData(Metrics.P1Priority, marks, bugs.filter(bug => bug.priority == Metrics.P1Priority)).foreach(data => dataSet.addValue(data._1, data._2, data._3))
    allOpenBugsChartData(Metrics.P2Priority, marks, bugs.filter(bug => bug.priority == Metrics.P2Priority)).foreach(data => dataSet.addValue(data._1, data._2, data._3))

    ReportField("image",
      MapValue(
        ReportField("content-type", StringValue("image/jpeg")),
        ReportField("content-value", StringValue(ChartGenerator.generateBase64OpenHighPriorityBugs(dataSet)))
      )
    )
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