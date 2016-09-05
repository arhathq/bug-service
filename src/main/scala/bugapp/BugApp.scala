package bugapp

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import bugapp.bugzilla.BugzillaRepository
import bugapp.http.HttpClient

import scala.concurrent.ExecutionContext

/**
  * @author Alexander Kuleshov
  */
object BugApp extends App with AkkaConfig with HttpConfig with BugzillaConfig {

  private implicit val system = ActorSystem("BugApp", akkaConfig)
  protected implicit val executor: ExecutionContext = system.dispatcher
  protected val log: LoggingAdapter = Logging(system, getClass)
  protected implicit val materializer: ActorMaterializer = ActorMaterializer()

  val httpClient = new HttpClient(bugzillaUrl, akkaConfig)

  val bugRepository = new BugzillaRepository(httpClient)

  val restService = new RestApiService(bugRepository)

  log.debug("Starting App...")

  Http().bindAndHandle(restService.routes, httpHost, httpPort)

}
