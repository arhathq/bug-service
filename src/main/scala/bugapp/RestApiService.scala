package bugapp

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives._
import bugapp.http.{AppRoute, CorsSupport}
import bugapp.repository.BugRepository

import scala.concurrent.ExecutionContext

class RestApiService(val bugRepository: BugRepository, val reportActor: ActorRef, val onlineActor: ActorRef)(implicit val system: ActorSystem, implicit val executionContext: ExecutionContext) extends CorsSupport {

  private val appRoutes = new AppRoute(bugRepository, reportActor, onlineActor)

  val routes = pathPrefix("api") {
    corsHandler {
      appRoutes.routes
    }
  }

}