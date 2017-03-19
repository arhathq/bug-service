package bugapp.http

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout
import bugapp.BugApp
import bugapp.Implicits._
import bugapp.report.OnlineReportActor.{CloseConversation, GetOnlineReport, JoinActor}
import bugapp.report.ReportActor._
import bugapp.report.ReportSender.{Ack, SendWeeklyReport}
import bugapp.report.converter.JsonReportDataConverter
import bugapp.report.{ReportTypes, model}
import bugapp.repository.BugRepository
import io.circe.generic.auto._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class AppRoute(val bugRepository: BugRepository, val reportActor: ActorRef, val onlineActor: ActorRef, val reportSender: ActorRef)(implicit val system: ActorSystem, implicit val executionContext: ExecutionContext, implicit val materializer: ActorMaterializer) extends ResponseSupport {

  lazy val log: LoggingAdapter = Logging(system, getClass)

  val routes =
    path("bugs" / IntNumber ) { weeks =>
      get {
        extractRequest { req =>
          val startDate = BugApp.fromDate(BugApp.toDate, weeks)
          sendResponse(bugRepository.getBugs(startDate))
        }
      }
    } ~
    path("report" / Segment / "weeks" / IntNumber) { (reportName, weeks) =>
      get {
        val reportDuration = 90.seconds
        withRequestTimeout(reportDuration) {
          extractRequest { req =>
            ReportTypes.from(reportName) match {
              case Left(error) => sendResponse(Future.failed(new RuntimeException(error)))
              case Right(reportType) =>
                implicit val timeout = Timeout(reportDuration)
                val endDate = BugApp.toDate
                val startDate = BugApp.fromDate(endDate, weeks)
                sendResponse(ask(reportActor, GetReport(reportType, startDate, endDate, weeks)).mapTo[ReportResult])
            }
          }
        }
      }
    } ~
    path("mail" / Segment) { mailType =>
      post {
        val reportDuration = 90.seconds
        withRequestTimeout(reportDuration) {
          extractRequest { req =>
            implicit val timeout = Timeout(reportDuration)
            val weeks = 15
            sendResponse((reportSender ? SendWeeklyReport(weeks)).mapTo[Ack])
          }
        }
      }
    } ~
    path("online" / Segment) { reportType =>
      get {
        handleWebSocketMessages {

          Flow[Message].collect {
            case TextMessage.Strict(msg) =>
              Try(msg.toInt) match {
                case Success(weeks) => createOnlineRequest(reportType, weeks)
                case Failure(t) => Future.failed(t)
              }
            case TextMessage.Streamed(stream) =>
              stream.
                limit(100).
                completionTimeout(5 seconds).
                runFold("")(_ + _).
                flatMap{ msg =>
                  createOnlineRequest(reportType, msg.toInt)
                }
          }.mapAsync(parallelism = 3)(identity).
            via(createActorFlow(reportType)).
            map {
              msg: model.ReportData =>
                TextMessage.Strict(new JsonReportDataConverter().convert(msg).toString())
            }
        }
      }
    }

  private def createOnlineRequest(reportName: String, weeks: Int): Future[GetOnlineReport] = {
    val endDate = BugApp.toDate
    val startDate = BugApp.fromDate(endDate, weeks)

    ReportTypes.from(reportName) match {
      case Left(error) => Future.failed(new RuntimeException(error))
      case Right(reportType) => Future.successful(GetOnlineReport(reportType, startDate, endDate, weeks))
    }
  }

  private def createActorFlow(reportName: String): Flow[GetOnlineReport, model.ReportData, Any] = {
    val in = Sink.actorRef(onlineActor, CloseConversation)
    val out = Source.actorRef(5, OverflowStrategy.fail).mapMaterializedValue { actor =>
      onlineActor ! JoinActor(actor, reportName)
      actor
    }
    Flow.fromSinkAndSource(in, out)
  }

  override def logger: LoggingAdapter = log

}