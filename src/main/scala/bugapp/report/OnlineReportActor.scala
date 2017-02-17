package bugapp.report

import java.time.OffsetDateTime

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import bugapp.BugApp
import bugapp.report.ReportActor.{ReportData, ReportError, ReportResult}
import bugapp.report.ReportDataBuilder.GetReportData
import bugapp.repository.BugRepository

import scala.collection.mutable
import scala.util.{Failure, Success}

/**
  * Created by arhathq on 16.01.2017.
  */
class OnlineReportActor(bugRepository: BugRepository, excludedComponents: Seq[String]) extends Actor with ActorLogging {
  import OnlineReportActor._

  private implicit val ec = context.dispatcher

  private val senders = mutable.Map.empty[String, ActorRef]
  private val reportDataBuilders = mutable.Map.empty[String, ActorRef]

  override def receive: Receive = {
    case GetOnlineReport(reportType, startDate, endDate, weekPeriod) =>
      val reportId = ReportActor.newReportId
      senders += reportId -> sender
      val bugsFuture = bugRepository.getBugs

      bugsFuture.onComplete {
        case Success(bugs) =>
          val reportDataBuilder = context.actorOf(ReportDataBuilder.props(self))
          reportDataBuilders += (reportId -> reportDataBuilder)
          val reportParams = Map[String, Any](
                      ReportParams.ReportType -> reportType,
                      ReportParams.StartDate -> startDate,
                      ReportParams.EndDate -> endDate,
                      ReportParams.BugtrackerUri -> BugApp.bugzillaUrl,
                      ReportParams.WeekPeriod -> weekPeriod,
                      ReportParams.ExcludedComponents -> excludedComponents
          )
          reportDataBuilder ! GetReportData(reportId, reportParams, bugs)

        case Failure(t) =>
          senders.remove(reportId) match {
            case Some(sender) =>
              sender ! ReportResult(report = None, error = Some(ReportError(reportId, t.getMessage)))
            case None =>
          }

      }

    case ReportData(reportId, reportType, result) =>
      reportDataBuilders.remove(reportId).foreach { reportDataBuilder =>
        reportDataBuilder ! PoisonPill
      }
      senders.remove(reportId) match {
        case Some(sender) => sender ! result
        case None =>
      }
  }
}

object OnlineReportActor {
  def props(bugRepository: BugRepository, excludedComponents: Seq[String]) =
    Props(classOf[OnlineReportActor], bugRepository, excludedComponents)

  case class GetOnlineReport(reportType: String, startDate: OffsetDateTime, endDate: OffsetDateTime, weekPeriod: Int)
  case class OnlineReport(reportId: String, report: model.ReportData)
}


