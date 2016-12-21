package bugapp

import java.time.OffsetDateTime
import java.time.temporal.{ChronoUnit, WeekFields}

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import bugapp.bugzilla.{BugzillaActor, BugzillaRepository, RepositoryEventBus}
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

  private val httpClient = new HttpClient(bugzillaUrl, akkaConfig)

  private val repositoryEventBus = new RepositoryEventBus(system)

  private val bugzillaActor = system.actorOf(BugzillaActor.props(httpClient, repositoryEventBus), "bugzillaActor")
  repositoryEventBus.subscribe(bugzillaActor, RepositoryEventBus.ResponseType)

  private val bugRepository = new BugzillaRepository(bugzillaActor)

  private val reportActor = system.actorOf(ReportActor.props(bugRepository, repositoryEventBus, excludedComponents ++ excludedProducts), "reportActor")
  repositoryEventBus.subscribe(reportActor, RepositoryEventBus.RequestType)

  private val restService = new RestApiService(bugRepository, reportActor)

  log.debug("Starting App...")

  Http().bindAndHandle(restService.routes, httpHost, httpPort).
    recover {
      case t: Throwable =>
        log.error("Error occurred during start. Stopping App...", t)
        system.terminate
        System.exit(0)
    }

  def toDate: OffsetDateTime = OffsetDateTime.now

  def fromDate(toDate: OffsetDateTime, weeksPeriod: Int): OffsetDateTime = {
    toDate.minusWeeks(weeksPeriod - 1).`with`(WeekFields.ISO.getFirstDayOfWeek).truncatedTo(ChronoUnit.DAYS)
  }
}