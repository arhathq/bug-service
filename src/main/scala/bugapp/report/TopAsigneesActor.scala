package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}

/**
  * @author Alexander Kuleshov
  */
class TopAsigneesActor(owner: ActorRef) extends Actor with ActorLogging {
  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val data =
        <top-asignees/>

      owner ! ReportDataResponse(reportId, data)
  }
}

object TopAsigneesActor {
  def props(owner: ActorRef) = Props(classOf[TopAsigneesActor], owner)
}