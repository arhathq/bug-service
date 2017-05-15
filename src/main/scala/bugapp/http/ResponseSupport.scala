package bugapp.http

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import de.heikoseeberger.akkahttpcirce.CirceSupport

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  *
  */
trait ResponseSupport extends CirceSupport with LoggerSupport {

  import io.circe.generic.auto._

  def sendResponse[T](eventualResult: Future[T])(implicit marshaller: T ⇒ ToResponseMarshallable): Route = {
    onComplete(eventualResult) {
      case Success(result) ⇒
        logger.debug(s"Response: $result")
        complete(result)
      case Failure(e) ⇒
        logger.error(s"Error: ${e.toString}")
        complete(ToResponseMarshallable(InternalServerError → ErrorMessage(e.getMessage)))
    }
  }
}

case class ErrorMessage(error: String)