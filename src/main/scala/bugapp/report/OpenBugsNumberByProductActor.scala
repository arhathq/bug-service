package bugapp.report

import akka.actor.{ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportActor.formatNumber
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.report.model.{ListValue, MapValue, ReportField, StringValue}

/**
  * @author Alexander Kuleshov
  */
class OpenBugsNumberByProductActor(owner: ActorRef) extends ReportWorker(owner) with ActorLogging {
  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, _, bugs) =>
      val bugsByProduct = bugs.filter(bug => bug.stats.status == Metrics.OpenStatus).groupBy(bug => bug.product)

      val bugCountByProductAndPriority = bugsByProduct.map {tuple =>
        val bugCountByPriority = tuple._2.groupBy(bug => bug.priority).map(tuple => (tuple._1, tuple._2.length))
        (tuple._1, bugCountByPriority)
      }

      val totalBugCountByPriority =
        bugCountByProductAndPriority.values.
          foldLeft(Map[String, Int]())((acc, el) => acc ++ el.map { case (k, v) => k -> (v + acc.getOrElse(k, 0)) })

      val productBugsValue =
        bugCountByProductAndPriority.toSeq.
          sortWith((tuple1, tuple2) => tuple1._2.values.sum > tuple2._2.values.sum).
          map(tuple => productBugsData(tuple._1, tuple._2)) :+
          productBugsData("Grand Total", totalBugCountByPriority)

      val data = model.ReportData("open-bugs-by-product",
        MapValue(
          ReportField("product-bugs", ListValue(productBugsValue: _*))
        )
      )

      owner ! ReportDataResponse(reportId, data)
  }

  def productBugsData(product: String, data: Map[String, Int]): MapValue = {
    val p1Count = data.getOrElse(Metrics.P1Priority, 0)
    val p2Count = data.getOrElse(Metrics.P2Priority, 0)
    val p3Count = data.getOrElse(Metrics.P3Priority, 0)
    val npCount = data.getOrElse(Metrics.NPPriority, 0)

    MapValue(
      ReportField("product", StringValue(product)),
      ReportField("np", StringValue(formatNumber(npCount))),
      ReportField("p1", StringValue(formatNumber(p1Count))),
      ReportField("p2", StringValue(formatNumber(p2Count))),
      ReportField("p3", StringValue(formatNumber(p3Count))),
      ReportField("total", StringValue(formatNumber(p1Count + p2Count + p3Count + npCount)))
    )
  }

}

object OpenBugsNumberByProductActor {
  def props(owner: ActorRef) = Props(classOf[OpenBugsNumberByProductActor], owner)
}
