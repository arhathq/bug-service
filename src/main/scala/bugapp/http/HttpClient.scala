package bugapp.http

import akka.actor.ActorSystem
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.http.scaladsl.model._
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.unmarshalling.{Unmarshal, _}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{BidiFlow, Flow, Sink, Source}
import bugapp.utils.DummySSLFactory
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * @author Alexander Kuleshov
  */
class HttpClient(url: String, config: Config)(implicit val s: ActorSystem, implicit val m: ActorMaterializer, implicit val ec: ExecutionContext) {
  val uri = Uri(url)

  lazy val hostName = uri.authority.host.address()
  lazy val port = uri.authority.port
  lazy val scheme = uri.scheme

  lazy val sslContext = new DummySSLFactory().getSSLContext

  val settings = ConnectionPoolSettings(config)

  def connectionFlow[Context]: Flow[(HttpRequest, Context), (Try[HttpResponse], Context), Any] = scheme match {
    case "http" =>
      Http().cachedHostConnectionPool[Context](hostName, port, settings)

    case "https" =>
      Http().cachedHostConnectionPoolHttps[Context](hostName, port, ConnectionContext.https(sslContext), settings)
  }

  def execute[Response](uri: Uri, method: HttpMethod)(implicit um: FromEntityUnmarshaller[Response]): Future[Response] = {

    val outbound = Flow[(Any, Any)].map {
      case (req, context) =>
        val httpRequest = HttpRequest(method, uri)
        (httpRequest, context)
    }

    val executionFlow = BidiFlow.fromFlows(outbound, inbound[Response, Any]).join(connectionFlow[Any])

    run[Response](executionFlow)(None)
  }

  private def inbound[Response, Context](implicit um: FromEntityUnmarshaller[Response]): Flow[(Try[HttpResponse], Context), (Try[Response], Context), Any] = {
    Flow[(Try[HttpResponse], Context)].map {
      case (tryResponse, context) =>
        tryResponse match {
          case Success(response) =>
              Unmarshal(response.entity).to[Response].map(response => (Try(response), context))
          case Failure(error) => Future.fromTry(Failure(error))
        }
    }.flatMapConcat(Source.fromFuture)
  }

  private def run[Response](flow: Flow[(Any, Any), (Try[Response], Any), Any])(request: Any): Future[Response] = {
    Source.single((request, None))
      .via(flow)
      .runWith(Sink.head)(m)
      .map(_._1)(ec)
      .flatMap(Future.fromTry)(ec)
  }

}
