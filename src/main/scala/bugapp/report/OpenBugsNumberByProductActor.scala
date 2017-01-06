package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportActor.formatNumber
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}

import scala.xml.Elem

/**
  * @author Alexander Kuleshov
  */
class OpenBugsNumberByProductActor(owner: ActorRef) extends Actor with ActorLogging {
  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val bugsByProduct = bugs.filter(bug => bug.stats.status == Metrics.OpenStatus).groupBy(bug => bug.product)

      val bugCountByProductAndPriority = bugsByProduct.map {tuple =>
        val bugCountByPriority = tuple._2.groupBy(bug => bug.priority).map(tuple => (tuple._1, tuple._2.length))
        (tuple._1, bugCountByPriority)
      }

      val totalBugCountByPriority =
        bugCountByProductAndPriority.values.
          foldLeft(Map[String, Int]())((acc, el) => acc ++ el.map { case (k, v) => k -> (v + acc.getOrElse(k, 0)) })

      val data =
        <open-bugs-by-product>
          {bugCountByProductAndPriority.map(tuple => productBugsElem(tuple._1, tuple._2))}
          {productBugsElem("Grand Total", totalBugCountByPriority)}
        </open-bugs-by-product>

      owner ! ReportDataResponse(reportId, data)
  }

  def productBugsElem(product: String, data: Map[String, Int]): Elem = {
    val p1Count = data.getOrElse(Metrics.P1Priority, 0)
    val p2Count = data.getOrElse(Metrics.P2Priority, 0)
    val p3Count = data.getOrElse(Metrics.P3Priority, 0)
    val npCount = data.getOrElse(Metrics.NPPriority, 0)

    <product-bugs>
      <product>{product}</product>
      <np>{formatNumber(npCount)}</np>
      <p1>{formatNumber(p1Count)}</p1>
      <p2>{formatNumber(p2Count)}</p2>
      <p3>{formatNumber(p3Count)}</p3>
      <total>{formatNumber(p1Count + p2Count + p3Count + npCount)}</total>
    </product-bugs>
  }

}

object OpenBugsNumberByProductActor {
  def props(owner: ActorRef) = Props(classOf[OpenBugsNumberByProductActor], owner)
}
