package bugapp

import akka.http.scaladsl.server.Directives._
import bugapp.http.{AppRoute, CorsSupport}
import bugapp.repository.BugRepository

import scala.concurrent.ExecutionContext

class RestApiService(val bugRepository: BugRepository)(implicit executionContext: ExecutionContext) extends CorsSupport {

  private val appRoutes = new AppRoute(bugRepository)

  val routes = pathPrefix("api") {
    corsHandler {
      appRoutes.routes
    }
  }

}