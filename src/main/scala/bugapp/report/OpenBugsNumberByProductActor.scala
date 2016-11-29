package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.report.ReportDataBuilder.{ReportData, ReportDataRequest, ReportDataResponse}

/**
  * @author Alexander Kuleshov
  */
class OpenBugsNumberByProductActor(owner: ActorRef) extends Actor with ActorLogging {
  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val data =
        <open-bugs-by-product/>

      owner ! ReportDataResponse(ReportData(reportId, reportParams(ReportParams.ReportType).asInstanceOf[String], data))
  }
}

object OpenBugsNumberByProductActor {
  def props(owner: ActorRef) = Props(classOf[OpenBugsNumberByProductActor], owner)
}
