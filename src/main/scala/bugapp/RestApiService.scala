package bugapp

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import bugapp.http.{AppRoute, CorsSupport}
import bugapp.repository.BugRepository

import scala.concurrent.ExecutionContext

/**
  *
  */
class RestApiService(val bugRepository: BugRepository, val reportActor: ActorRef, val onlineActor: ActorRef, val reportSender: ActorRef)(implicit val system: ActorSystem, implicit val executionContext: ExecutionContext, implicit val materializer: ActorMaterializer) extends CorsSupport {

  private val appRoutes = new AppRoute(bugRepository, reportActor, onlineActor, reportSender)

  val routes = pathPrefix("api") {
    corsHandler {
      appRoutes.routes
    }
  }

}