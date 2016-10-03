package bugapp.http

import java.time.LocalDate

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import bugapp.Implicits._
import bugapp.report.ReportActor._
import bugapp.repository.BugRepository
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

class AppRoute(val bugRepository: BugRepository, val reportActor: ActorRef)(implicit val system: ActorSystem, implicit val executionContext: ExecutionContext) extends ResponseSupport {

  lazy val log: LoggingAdapter = Logging(system, getClass)

  implicit val timeout = Timeout(5 seconds)

  val routes =
    path("bugs" / IntNumber ) { weeks =>
      get {
        extractRequest { req =>
          sendResponse(bugRepository.getBugs(LocalDate.now.minusWeeks(weeks)))
        }
      }
    } ~
    path("report" / IntNumber ) { weeks =>
      get {
        extractRequest { req =>
          sendResponse(ask(reportActor, GetReport(weeks)).mapTo[ReportResult])
        }
      }
    }

  override def logger: LoggingAdapter = log

}