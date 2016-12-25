package bugapp.report

import akka.actor.{ActorContext, ActorRef}
import bugapp.repository.FileEmployeeRepository

/**
  * Created by arhathq on 20.12.2016.
  */
class ReportWorkers(context: ActorContext) {

  private val self = context.self

  private val employeeRepository = new FileEmployeeRepository

  def create(reportType: String): Set[ActorRef] = reportType match {
    case "weekly" => Set(
      context.actorOf(AllOpenBugsNumberByPriorityActor.props(self)),
      context.actorOf(OpenTopBugListActor.props(self)),
      context.actorOf(ReportersBugNumberByPeriodActor.props(self, employeeRepository, 15)),
      context.actorOf(ReportersBugNumberByPeriodActor.props(self, employeeRepository, 1)),
      context.actorOf(PrioritizedBugNumberByThisWeekActor.props(self)),
      context.actorOf(OpenBugsNumberByProductActor.props(self)),
      context.actorOf(BugsByPeriodChartActor.props(self, 15)),
      context.actorOf(TopAssigneesActor.props(self)),
      context.actorOf(WeeklySummaryReportActor.props(self))
    )
    case "sla" => Set(
      context.actorOf(SlaReportActor.props(self)),
      context.actorOf(BugsOutSlaActor.props(self))
    )
    case _ => Set()
  }


}
