package bugapp.http

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import bugapp.repository.BugRepository
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class AppRoute(private val bugRepository: BugRepository)(implicit executionContext: ExecutionContext) extends CirceSupport {

  val routes =
    path("bugs") {
      get {
        onComplete(bugRepository.getBugs().map(_.asJson)) {
          case Success(value) => complete(value)
          case Failure(ex)    =>
            complete(HttpResponse(InternalServerError, entity = ErrorMessage(ex.getMessage).asJson.noSpaces))
        }
      }
    }
}

case class ErrorMessage(error: String)