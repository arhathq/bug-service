package bugapp

import java.time.OffsetDateTime
import java.time.temporal.WeekFields

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

  private val httpClient = new HttpClient(bugzillaUrl, akkaConfig)

  private val bugzillaActor = system.actorOf(BugzillaActor.props(httpClient), "bugzillaActor")

  private val bugRepository = new BugzillaRepository(bugzillaActor)

  private val reportActor = system.actorOf(ReportActor.props(bugRepository), "reportActor")

  private val restService = new RestApiService(bugRepository, reportActor)

  log.debug("Starting App...")

  Http().bindAndHandle(restService.routes, httpHost, httpPort)

  def fromDate(toDate: OffsetDateTime, weeksPeriod: Int): OffsetDateTime = {
    toDate.minusWeeks(weeksPeriod).`with`(WeekFields.ISO.dayOfWeek(), 1).
      withHour(0).withMinute(0).withSecond(0).withNano(0)
  }
}
