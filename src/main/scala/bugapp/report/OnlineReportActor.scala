package bugapp.report

import java.time.OffsetDateTime
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import bugapp.BugApp
import bugapp.report.ReportActor.{ReportData, ReportError, ReportResult}
import bugapp.report.ReportDataBuilder.GetReportData
import bugapp.report.ReportTypes.ReportType
import bugapp.repository.BugRepository

import scala.collection.mutable
import scala.util.{Failure, Success}

/**
  * @author Alexander Kuleshov
  */
class OnlineReportActor(bugRepository: BugRepository, excludedComponents: Seq[String]) extends Actor with ActorLogging {
  import OnlineReportActor._

  private implicit val ec = context.dispatcher

  private var participants: Map[String, Set[ActorRef]] = Map.empty[String, Set[ActorRef]]
  private val reportDataBuilders = mutable.Map.empty[String, ActorRef]

  override def receive: Receive = {
    case request@GetOnlineReport(reportType, startDate, endDate, weekPeriod) =>
      val reportId = request.reportId
      val bugsFuture = bugRepository.getBugs

      bugsFuture.onComplete {
        case Success(bugs) =>
          val reportDataBuilder = context.actorOf(ReportDataBuilder.props(self, WorkersFactory.Online))
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
          participants.values.foreach { senders =>
            log.debug("Send another result to " + senders)
            senders.foreach(sender => sender ! ReportResult(report = None, error = Some(ReportError(reportId, t.getMessage))))
          }

      }

    case ReportData(reportId, reportType, result) =>
      reportDataBuilders.remove(reportId).foreach { reportDataBuilder =>
        reportDataBuilder ! PoisonPill
      }
      participants get reportType.name match {
        case Some(actors) =>
          actors.foreach { sender =>
            log.debug("Send another result to " + sender)
            sender ! result
          }

        case None =>
      }

    case JoinActor(ref, reportName) =>
      log.debug("JoinActor " + ref)
      participants get reportName match {
        case Some(actors) => participants += (reportName -> (actors + ref))
        case None => participants += (reportName -> Set(ref))
      }

    case CloseConversation =>
      participants --= participants.keys
      log.debug("CloseConversation")
  }
}

object OnlineReportActor {
  def props(bugRepository: BugRepository, excludedComponents: Seq[String]) =
    Props(classOf[OnlineReportActor], bugRepository, excludedComponents)

  case class GetOnlineReport(reportType: ReportType, startDate: OffsetDateTime, endDate: OffsetDateTime, weekPeriod: Int) {
    val reportId: String = UUID.randomUUID().toString
  }
  case class OnlineReport(reportId: String, report: model.ReportData)

  case class JoinActor(ref: ActorRef, reportName: String)
  case object CloseConversation
}


