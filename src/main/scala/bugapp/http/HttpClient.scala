package bugapp.http

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
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

  private val logger: LoggingAdapter = Logging(s, getClass)

  val uri = Uri(url)

  val hostName = uri.authority.host.address()
  val port = uri.authority.port
  val scheme = uri.scheme

  lazy val sslContext = new DummySSLFactory().getSSLContext

  val settings = ConnectionPoolSettings(config)

  def connectionFlow[Context]: Flow[(HttpRequest, Context), (Try[HttpResponse], Context), Any] = scheme match {
    case "http" =>
      Http().cachedHostConnectionPool[Context](hostName, port, settings)

    case "https" =>
      Http().cachedHostConnectionPoolHttps[Context](hostName, port, ConnectionContext.https(sslContext), settings)
  }

  def execute[Response](uri: Uri, method: HttpMethod)(implicit um: FromEntityUnmarshaller[Response]): Future[Response] = {

    log("Http Request:", uri.queryString())

    val outbound = Flow[(Any, Any)].map {
      case (req, context) =>
        val httpRequest = HttpRequest(method, uri)
        (httpRequest, context)
    }

    val executionFlow = BidiFlow.fromFlows(outbound, inbound[Response, Any]).join(connectionFlow[Any])

    run[Response](executionFlow)
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

  private def run[Response](flow: Flow[(Any, Any), (Try[Response], Any), Any]): Future[Response] = {
    Source.single((None, None))
      .via(flow)
      .runWith(Sink.head)(m)
      .map(res => {log("Http Response:", res._1.getOrElse("")); res._1})(ec)
      .flatMap(Future.fromTry)(ec)
  }

  private def log(message: String, value: Any): Unit = {
    val valueStr = value.toString
    logger.debug(s"$message ${valueStr.substring(0, valueStr.length.min(255))}")
  }
}
