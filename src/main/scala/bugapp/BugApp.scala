package bugapp

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import bugapp.repository.BugzillaRepository

import scala.concurrent.ExecutionContext

/**
  * @author Alexander Kuleshov
  */
object BugApp extends App with Config {

  private implicit val system = ActorSystem("BugApp", akkaConfig)
  protected implicit val executor: ExecutionContext = system.dispatcher
  protected val log: LoggingAdapter = Logging(system, getClass)
  protected implicit val materializer: ActorMaterializer = ActorMaterializer()

  val bugRepository = new BugzillaRepository

  val restService = new RestApiService(bugRepository)

  Http().bindAndHandle(restService.routes, httpHost, httpPort)

}
