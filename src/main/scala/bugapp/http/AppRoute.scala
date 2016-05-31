package bugapp.http

import akka.http.scaladsl.server.Directives._
import bugapp.repository.BugRepository
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.ExecutionContext

class AppRoute(private val bugRepository: BugRepository)(implicit executionContext: ExecutionContext) extends CirceSupport {

  val routes = pathPrefix("bugs") {
    pathEndOrSingleSlash {
      get {
        complete(bugRepository.getBugs.map(_.asJson))
      }
    }
  }

}
