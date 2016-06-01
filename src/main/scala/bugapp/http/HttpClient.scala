package bugapp.http

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.{Unmarshal, _}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{BidiFlow, Flow, Sink, Source}
import bugapp.utils.DummySSLFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * @author Alexander Kuleshov
  */
class HttpClient(url: String)(implicit val s: ActorSystem, implicit val m: ActorMaterializer, implicit val ec: ExecutionContext) {
  val uri = Uri(url)

  lazy val hostName = uri.authority.host.address()
  lazy val port = uri.authority.port
  lazy val scheme = uri.scheme

  lazy val sslContext = new DummySSLFactory().getSSLContext

  def connectionFlow[Context]: Flow[(HttpRequest, Context), (Try[HttpResponse], Context), Any] = scheme match {
    case "http" => Http().cachedHostConnectionPool[Context](hostName, port)
    case "https" =>
      Http().cachedHostConnectionPoolHttps[Context](hostName, port, ConnectionContext.https(sslContext))
  }

  def execute[Request, Response](uri: String, request: Request)(implicit um: FromEntityUnmarshaller[Response]): Future[Response] = {

    val outbound = Flow[(Request, Any)].map {
      case (req, context) =>
        val httpRequest = HttpRequest(uri = uri, method = HttpMethods.GET)
        (httpRequest, context)
    }

    val flow = BidiFlow.fromFlows(outbound, inbound[Response, Any]).join(connectionFlow[Any])

    run[Request, Response](flow)(request)
  }

  def execute[Request, Response](request: Request, flow: Flow[(Any, Any), (HttpRequest, Any), NotUsed])(implicit um: FromEntityUnmarshaller[Response]): Future[Response] = {

    val executionFlow = BidiFlow.fromFlows(flow, inbound[Response, Any]).join(connectionFlow[Any])

    run[Request, Response](executionFlow)(request)
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

  private def run[Request, Response](flow: Flow[(Request, Any), (Try[Response], Any), Any])(request: Request): Future[Response] = {
    Source.single((request, None))
      .via(flow)
      .runWith(Sink.head)(m)
      .map(_._1)(ec)
      .flatMap(Future.fromTry)(ec)
  }

}
