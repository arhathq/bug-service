package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}

import scala.xml._

/**
  * @author Alexander Kuleshov
  */
class AllOpenBugsNumberByPriorityActor(owner: ActorRef) extends Actor with ActorLogging {

  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val elements: Seq[(String, Int)] =
        Seq(
          ("period1", 1), ("period2", 2), ("period3", 3),
          ("period4", 4), ("period5", 5), ("period6", 6)
        )

        val data =
            <all-open-bugs>
              {prioritizedBugsElem(Metrics.P1Priority, elements)}
              {prioritizedBugsElem(Metrics.P2Priority, elements.map(e => (e._1, e._2 - 1)))}
              {prioritizedBugsElem(Metrics.P3Priority, elements.map(e => (e._1, e._2 * 2)))}
              {prioritizedBugsElem(Metrics.NPPriority, elements.map(e => (e._1, 0)))}
            </all-open-bugs>

         owner ! ReportDataResponse(reportId, data)
  }

  def prioritizedBugsElem(priority: String, data: Seq[(String, Int)]): Elem = {
    <prioritized-bugs>
      <priority>{priority}</priority>
      {data.map(tuple => Elem.apply(null, s"${tuple._1}", Null, TopScope, true, Text(tuple._2.toString)))}
      <total>{data.map(_._2).sum}</total>
    </prioritized-bugs>
  }
}

object AllOpenBugsNumberByPriorityActor {
  def props(owner: ActorRef) = Props(classOf[AllOpenBugsNumberByPriorityActor], owner)
}