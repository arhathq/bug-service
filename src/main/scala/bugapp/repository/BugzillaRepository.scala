package bugapp.repository

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import bugapp.{Bug, Config}
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import com.typesafe.sslconfig.ssl.SSLLooseConfig

import scala.concurrent.{ExecutionContext, Future}

class BugzillaRepository(implicit val s: ActorSystem, implicit val m: ActorMaterializer, implicit val ec: ExecutionContext) extends BugRepository with Config {

  val looseConfig = SSLLooseConfig().withAcceptAnyCertificate(true).
    withDisableHostnameVerification(true).
    withAllowLegacyHelloMessages(Some(true)).
    withAllowUnsafeRenegotiation(Some(true)).
    withAllowWeakCiphers(true).
    withAllowWeakProtocols(true).
    withDisableSNI(true)

  val badSslConfig = AkkaSSLConfig()
  val httpsCtx = Http().createClientHttpsContext(badSslConfig)

  val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
    Http().outgoingConnectionHttps(bugzillaUrl, connectionContext = httpsCtx)

  val responseFuture: Future[HttpResponse] =
    Source.single(HttpRequest(uri = "/jsonrpc.cgi"))
      .via(connectionFlow)
      .runWith(Sink.head)

  override def getBugs: Future[Seq[Bug]] = {
    responseFuture.map {
      case HttpResponse(StatusCodes.OK, headers, entity, _) =>
        List()
      case HttpResponse(code, _, _, _) =>
        throw new RuntimeException("Request failed, response code: " + code)
    }
  }
}
