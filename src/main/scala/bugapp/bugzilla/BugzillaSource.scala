package bugapp.bugzilla

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.stage.{AsyncCallback, GraphStage, GraphStageLogic, OutHandler}
import akka.util.ByteString
import bugapp.BugzillaConfig
import bugapp.utils.DummySSLFactory

import scala.util.{Failure, Success, Try}

/**
  * @author Alexander Kuleshov
  */
class BugzillaSource(val startDate: Option[OffsetDateTime] = None, val limit: Int = 500, val offset: Int = 0)(implicit val s: ActorSystem) extends GraphStage[SourceShape[ByteString]] with BugzillaConfig {
  val out: Outlet[ByteString] = Outlet("BugzillaSource")
  override val shape: SourceShape[ByteString] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      private var counter = 0

      private var futureHttpResponseCallback: AsyncCallback[Try[HttpResponse]] = _
      private var futureResponoseCallback: AsyncCallback[Try[ByteString]] = _

      private val logger: LoggingAdapter = Logging(s, getClass)

      private val dummyCtx = ConnectionContext.https(new DummySSLFactory().getSSLContext)

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {

          implicit val ec = materializer.executionContext

          val fromDate = startDate match {
            case Some(value) => value
            case None => searchStartDate
          }

          val bugzillaParams = new BugzillaParams(bugzillaUsername, bugzillaPassword, Some(fromDate), limit = Some(limit), offset = Some(counter))
          val bugzillaRequest = BugzillaRequest("Bug.search", bugzillaParams)

          val httpRequest = HttpRequest(uri = BugzillaRequest.jsonrpc(bugzillaUrl, bugzillaRequest))
          val future = Http().singleRequest(httpRequest, dummyCtx)(materializer)

          futureHttpResponseCallback = getAsyncCallback[Try[HttpResponse]](tryPushAfterRequest)
          future.onComplete(futureHttpResponseCallback.invoke)

          counter += limit
        }
      })

      private def tryPushAfterRequest(responseOrFailure: Try[HttpResponse]): Unit = responseOrFailure match {
        case Success(response) => response match {
          case HttpResponse(StatusCodes.OK, headers, entity, _) =>
            implicit val ec = materializer.executionContext
            val futureResponse = entity.dataBytes.runFold(ByteString(""))(_ ++ _)(materializer)
            futureResponoseCallback = getAsyncCallback[Try[ByteString]](tryPushAfterGettingResponse)
            futureResponse.onComplete(futureResponoseCallback.invoke)

          case resp @ HttpResponse(code, _, _, _) =>
            logger.warning(s"Invalid http response code: $code. Source will be closed")
            resp.discardEntityBytes()(materializer)
            completeStage()
        }
        case Failure(failure) =>
          logger.error(failure, s"Error")
          failStage(failure)
      }

      private def tryPushAfterGettingResponse(responseOrFailure: Try[ByteString]): Unit = responseOrFailure match {
        case Success(response) =>
          logger.debug(s"Tick (responseLength=${response.length})")
          push(out, response)
        case Failure(failure) =>
          logger.error(failure, s"Error")
          failStage(failure)
      }

    }
}