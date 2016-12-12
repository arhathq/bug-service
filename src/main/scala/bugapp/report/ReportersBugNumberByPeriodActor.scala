package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.repository.EmployeeRepository

/**
  * @author Alexander Kuleshov
  */
class ReportersBugNumberByPeriodActor(owner: ActorRef, repository: EmployeeRepository) extends Actor with ActorLogging {
  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>
      val data =
          <reporter-bugs-by-15-weeks>
            <reporter-bugs/>
          </reporter-bugs-by-15-weeks>

      owner ! ReportDataResponse(reportId, data)
  }
}

object ReportersBugNumberByPeriodActor {
  def props(owner: ActorRef, repository: EmployeeRepository) = Props(classOf[ReportersBugNumberByPeriodActor], owner, repository)
}
