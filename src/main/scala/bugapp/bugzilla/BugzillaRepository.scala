package bugapp.bugzilla

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.model.{HttpMethods, Uri}
import akka.stream.ActorMaterializer
import bugapp._
import bugapp.http.HttpClient
import bugapp.repository.{Bug, BugRepository}

import scala.concurrent.{ExecutionContext, Future}

class BugzillaRepository(httpClient: HttpClient)(implicit val s: ActorSystem, implicit val m: ActorMaterializer, implicit val ec: ExecutionContext) extends BugRepository with BugzillaConfig {

  protected val log: LoggingAdapter = Logging(s, getClass)

  /**
   * method "Bug.search"
   * params   [{"Bugzilla_login":"user","Bugzilla_password":"password","status":["RESOLVED"],"cf_target_milestone":["2016"],"cf_production":["Production"]}]
   * {"error":{"message":"When using JSON-RPC over GET, you must specify a 'method' parameter. See the documentation at docs/en/html/api/Bugzilla/WebService/Server/JSONRPC.html","code":32000},"id":"http://192.168.0.2","result":null}
   */
  override def getBugs(statuses: List[String], milestones: List[String], environments: List[String]): Future[Seq[Bug]] = {
    val params = BugzillaParams(
      bugzillaUsername,
      bugzillaPassword,
      Some("2016-04-01")
    )
    getBugs(BugzillaRequest("Bug.search", params)).map { s =>
      Seq[Bug]()
    }
  }

  override def getOpenBugs(statuses: List[String], priorities: List[String]): Future[Seq[Bug]] = {
    val params = BugzillaParams(
      bugzillaUsername,
      bugzillaPassword,
      Some("2016-04-01")
    )
    getBugs(BugzillaRequest("Bug.search", params)).map { s =>
      Seq[Bug]()
    }
  }

  private def toJsonString(params: BugzillaParams): String = {
    import io.circe.generic.auto._
    import io.circe.syntax._

    List(params).asJson.noSpaces
  }

  private def uri(request: BugzillaRequest): Uri = {
    Uri(bugzillaUrl).
      withPath(Uri.Path("/jsonrpc.cgi")).
      withQuery(Uri.Query(
        Map(
          "method" -> request.method,
          "params" -> toJsonString(request.params)
        )
      ))
  }

  private def getBugs(request: BugzillaRequest): Future[String] = {
//    import io.circe.generic.auto._
//    import io.circe.syntax._


    httpClient.execute[String](uri(request), HttpMethods.GET).map { response =>
      log.debug(response.toString)
      response
    }
  }

  //query1
  val openStatuses = List("UNCOFIRMED", "NEW", "ASSIGNED", "IN_PROGRESS", "BLOCKED", "PROBLEM_DETERMINED", "REOPENED")
  val openPriorities = List("P1", "P2")
  val environment = "Production"
  val excludedProducts = List("CRF Hot Deploy - Prod DB", "Ecomm Deploy - Prod DB")
  val excludedComponents = List("Dataload Failed", "New Files Arrived", "Data Consistency")

}

