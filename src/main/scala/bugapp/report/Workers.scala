package bugapp.report

import akka.actor.{ActorContext, ActorRef}
import bugapp.repository.FileEmployeeRepository

/**
  * Created by arhathq on 20.12.2016.
  */

object WorkersFactory {
  sealed trait WorkersType
  case object Online extends WorkersType
  case object Report extends WorkersType

  def createWorkers(id: WorkersType, context: ActorContext) = id match {
    case Report => new ReportWorkers(context)
    case Online => new OnlineReportWorkers(context)
  }
}

trait Workers {
  def create(reportType: String): Set[ActorRef]
}

class ReportWorkers(context: ActorContext) extends Workers {

  private val self = context.self

  private val employeeRepository = new FileEmployeeRepository

  override def create(reportType: String): Set[ActorRef] = reportType match {
    case "weekly" => Set(
      context.actorOf(AllOpenBugsNumberByPriorityActor.props(self)),
      context.actorOf(AllOpenBugsNumberByPriorityChartActor.props(self)),
      context.actorOf(OpenTopBugListActor.props(self)),
      context.actorOf(ReportersBugNumberByPeriodActor.props(self, employeeRepository, 15)),
      context.actorOf(ReportersBugNumberByPeriodActor.props(self, employeeRepository, 1)),
      context.actorOf(PrioritizedBugNumberByThisWeekActor.props(self)),
      context.actorOf(OpenBugsNumberByProductActor.props(self)),
      context.actorOf(BugsByPeriodChartActor.props(self, 15)),
      context.actorOf(TopAssigneesActor.props(self)),
      context.actorOf(WeeklySummaryReportActor.props(self)),
      context.actorOf(WeeklySummaryChartActor.props(self))
    )
    case "sla" => Set(
      context.actorOf(SlaReportActor.props(self)),
      context.actorOf(BugsOutSlaActor.props(self))
    )
    case _ => Set.empty[ActorRef]
  }
}

class OnlineReportWorkers(val context: ActorContext) extends Workers {
  override def create(reportType: String): Set[ActorRef] = reportType match {
    case _ => Set.empty[ActorRef]
  }
}