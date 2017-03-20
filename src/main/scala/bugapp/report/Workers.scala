package bugapp.report

import akka.actor.{ActorContext, ActorRef}
import bugapp.report.ReportTypes.{ReportType, SlaReport, WeeklyReport}
import bugapp.repository.FileEmployeeRepository

/**
  * Created by arhathq on 20.12.2016.
  */

object WorkersFactory {
  sealed trait WorkersType
  case object Online extends WorkersType
  case object Report extends WorkersType

  def createWorkers(id: WorkersType, context: ActorContext): Workers = id match {
    case Report => new ReportWorkers(context)
    case Online => new OnlineReportWorkers(context)
  }
}

trait Workers {
  def create(reportType: ReportType): Set[ActorRef]
}

class ReportWorkers(context: ActorContext) extends Workers {

  private val self = context.self

  private val employeeRepository = new FileEmployeeRepository

  override def create(reportType: ReportType): Set[ActorRef] = reportType match {
    case WeeklyReport => Set(
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
    case SlaReport => Set(
      context.actorOf(SlaReportActor.props(self)),
      context.actorOf(SlaChartActor.props(self)),
      context.actorOf(BugsOutSlaActor.props(self)),
      context.actorOf(BugsOutSlaChartActor.props(self))
    )
    case _ => Set.empty[ActorRef]
  }
}

class OnlineReportWorkers(val context: ActorContext) extends Workers {

  private val self = context.self

  override def create(reportType: ReportType): Set[ActorRef] = reportType match {
    case SlaReport => Set(
      context.actorOf(SlaChartActor.props(self, renderChart = false)),
      context.actorOf(BugsOutSlaChartActor.props(self, renderChart = false)),
      context.actorOf(WeeklySummaryChartActor.props(self, renderChart = false))
    )
    case _ => Set.empty[ActorRef]
  }
}