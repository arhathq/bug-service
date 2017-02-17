package bugapp.http

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import bugapp.BugApp
import bugapp.Implicits._
import bugapp.report.converter.JsonReportDataConverter._
import bugapp.report.OnlineReportActor.GetOnlineReport
import bugapp.report.ReportActor._
import bugapp.report.model
import bugapp.repository.BugRepository
import io.circe.generic.auto._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

class AppRoute(val bugRepository: BugRepository, val reportActor: ActorRef, val onlineActor: ActorRef)(implicit val system: ActorSystem, implicit val executionContext: ExecutionContext) extends ResponseSupport {

  lazy val log: LoggingAdapter = Logging(system, getClass)

  val routes =
    path("bugs" / IntNumber ) { weeks =>
      get {
        extractRequest { req =>
          val startDate = BugApp.fromDate(BugApp.toDate, weeks)
          sendResponse(bugRepository.getBugs(startDate))
        }
      }
    } ~
    path("report" / Segment / "weeks" / IntNumber) { (reportType, weeks) =>
      get {
        val reportDuration = 90.seconds
        withRequestTimeout(reportDuration) {
          extractRequest { req =>
            implicit val timeout = Timeout(reportDuration)
            val endDate = BugApp.toDate
            val startDate = BugApp.fromDate(endDate, weeks)
            sendResponse(ask(reportActor, GetReport(reportType, startDate, endDate, weeks)).mapTo[ReportResult])
          }
        }
      }
    } ~
    path("online" / Segment / "weeks" / IntNumber) { (reportType, weeks) =>
      get {
        val reportDuration = 5.seconds
        withRequestTimeout(reportDuration) {
          extractRequest { req =>
            implicit val timeout = Timeout(reportDuration)
            val endDate = BugApp.toDate
            val startDate = BugApp.fromDate(endDate, weeks)

            sendResponse(ask(onlineActor, GetOnlineReport(reportType, startDate, endDate, weeks)).mapTo[model.ReportData])
          }
        }
      }
    }

  override def logger: LoggingAdapter = log

}