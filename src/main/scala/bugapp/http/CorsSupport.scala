package bugapp.http

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCode.int2StatusCode
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Route}

trait CorsSupport {

  private def addAccessControlHeaders: Directive0 = mapResponseHeaders { headers ⇒
      `Access-Control-Allow-Origin`.* +:
      `Access-Control-Allow-Credentials`(true) +:
      `Access-Control-Allow-Headers`("Authorization", "Content-Type", "X-Requested-With") +:
      headers
  }

  private def preflightRequestHandler: Route = options {
    complete(HttpResponse(200).withHeaders(
      `Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE)
    ))
  }

  def corsHandler(r: Route) = addAccessControlHeaders {
    preflightRequestHandler ~ r
  }

}