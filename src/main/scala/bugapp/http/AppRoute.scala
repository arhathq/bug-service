package bugapp.http

import akka.actor.{ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import bugapp.report.ReportProtocol.GenerateReport
import bugapp.report.ReportActor
import bugapp.repository.BugRepository

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

class AppRoute(private val bugRepository: BugRepository)(implicit val system: ActorSystem, implicit val executionContext: ExecutionContext) extends ResponseSupport {

  import io.circe.generic.auto._

  lazy val log: LoggingAdapter = Logging(system, getClass)

  val reportActor = system.actorOf(Props(new ReportActor(bugRepository)), "reportActor")

  implicit val timeout = Timeout(5 seconds)

  val routes =
    path("bugs") {
      get {
        extractRequest { req =>
          sendResponse(bugRepository.getBugs())
        }
      }
    } ~
    path("openbugs") {
      get {
        extractRequest { req =>
          sendResponse(bugRepository.getOpenBugs())
        }
      }
    } ~
    path("report" / IntNumber ) { weeks =>
      get {
        extractRequest { req =>
          sendResponse(ask(reportActor, GenerateReport(weeks)).mapTo[String])
        }
      }
    }

  override def logger: LoggingAdapter = log

}