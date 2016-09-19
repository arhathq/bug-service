package bugapp

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import bugapp.bugzilla.{BugzillaActor, BugzillaRepository}
import bugapp.http.HttpClient
import bugapp.report.ReportActor

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

  val bugzillaActor = system.actorOf(BugzillaActor.props(httpClient), "bugzillaActor")

  val bugRepository = new BugzillaRepository(bugzillaActor)

  val reportActor = system.actorOf(ReportActor.props(bugRepository), "reportActor")

  val restService = new RestApiService(bugRepository, reportActor)

  log.debug("Starting App...")

  Http().bindAndHandle(restService.routes, httpHost, httpPort)

}
