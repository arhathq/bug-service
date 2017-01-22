package bugapp.bugzilla

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.stage.{AsyncCallback, GraphStage, GraphStageLogic, OutHandler}
import com.typesafe.sslconfig.akka.AkkaSSLConfig

import scala.util.{Failure, Success, Try}

/**
  * Created by arhathq on 21.01.2017.
  */
class BugzillaSource(val offset: Int)(implicit val s: ActorSystem) extends GraphStage[SourceShape[String]] {
  val out: Outlet[String] = Outlet("BugzillaSource")
  override val shape: SourceShape[String] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      private var counter = 1
      private var futureCallback: AsyncCallback[Try[HttpResponse]] = _

      private val badSslConfig = AkkaSSLConfig().mapSettings(s => s.withLoose(s.loose.withDisableSNI(true)))
      private val badCtx = Http().createClientHttpsContext(badSslConfig)

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {

          implicit val ec = materializer.executionContext

          val httpRequest = HttpRequest(uri = s"http://jsonplaceholder.typicode.com/posts/$counter")
          val future = Http().singleRequest(httpRequest, badCtx)(materializer)

          futureCallback = getAsyncCallback[Try[HttpResponse]](tryPushAfterRequest)
          future.onComplete(futureCallback.invoke)

          counter += offset
        }
      })


      private def tryPushAfterRequest(responseOrFailure: Try[HttpResponse]): Unit = responseOrFailure match {
        case Success(response) =>
          push(out, response.entity.toString)

        case Failure(failure) =>
          println(s"Error $failure")
          failStage(failure)
      }

    }
}