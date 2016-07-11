package bugapp.http

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import bugapp.report.ReportProtocol.GenerateReport
import bugapp.report.ReportActor
import bugapp.repository.BugRepository
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class AppRoute(private val bugRepository: BugRepository)(implicit val system: ActorSystem, implicit val executionContext: ExecutionContext) extends CirceSupport {

  val reportActor = system.actorOf(Props(new ReportActor(bugRepository)), "reportActor")

  implicit val timeout = Timeout(5 seconds)

  val routes =
    path("bugs") {
      get {
        onComplete(bugRepository.getBugs().map(_.asJson)) {
          case Success(value) => complete(value)
          case Failure(ex)    =>
            complete(HttpResponse(InternalServerError, entity = ErrorMessage(ex.getMessage).asJson.noSpaces))
        }
      }
    } ~
    path("report" / IntNumber) { weeks =>
      get {
        onComplete(ask(reportActor, GenerateReport(weeks)).mapTo[String]) {
          case Success(value) => complete(value)
          case Failure(ex)    =>
            complete(HttpResponse(InternalServerError, entity = ErrorMessage(ex.getMessage).asJson.noSpaces))
        }

      }
    }
}

case class ErrorMessage(error: String)