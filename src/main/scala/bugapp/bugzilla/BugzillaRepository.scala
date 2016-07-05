package bugapp.bugzilla

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import bugapp._
import bugapp.http.HttpClient
import bugapp.repository.{Bug, BugRepository, BugsError}
import de.heikoseeberger.akkahttpcirce.CirceSupport

import scala.concurrent.{ExecutionContext, Future}

class BugzillaRepository(implicit val s: ActorSystem, implicit val m: ActorMaterializer, implicit val ec: ExecutionContext) extends BugRepository with Config with CirceSupport {

  val httpClient = new HttpClient(bugzillaUrl)

  /**
   * method "Bug.search"
   * params   [{"Bugzilla_login":"user","Bugzilla_password":"password","status":["RESOLVED"],"cf_target_milestone":["2016"],"cf_production":["Production"]}]
   * {"error":{"message":"When using JSON-RPC over GET, you must specify a 'method' parameter. See the documentation at docs/en/html/api/Bugzilla/WebService/Server/JSONRPC.html","code":32000},"id":"http://192.168.0.2","result":null}
   */
  override def getBugs(statuses: List[String], milestones: List[String], environments: List[String]): Future[Seq[Bug]] = {
    val params = BugzillaParams.create(
      bugzillaUsername,
      bugzillaPassword,
      List("RESOLVED","VERIFIED","CLOSED"),
      List("2016.1.0","2016.2.0","2016.2.0+Dev1","2016.2.0+Dev2","2016.2.0+Dev3","2016.2.0+Dev4","2016.2.0+Dev5","2016.2.1","2016.3.0"))
    getBugs(BugzillaRequest("Bug.search", params))
  }

  private def getBugs(request: BugzillaRequest): Future[Seq[Bug]] = {
    import io.circe.generic.auto._
    import io.circe.syntax._

    val getBugsUri = Uri(bugzillaUrl).
      withPath(Uri.Path("/jsonrpc.cgi")).
      withQuery(Uri.Query(
        Map(
          "method" -> request.method,
          "params" -> List(request.params).asJson.noSpaces
        )
      ))

    val outbound = Flow[(Any, Any)].map {
      case (req, context) =>
        val httpRequest = HttpRequest(uri = getBugsUri, method = HttpMethods.GET)
        (httpRequest, context)
    }

    httpClient.execute[Any, BugzillaResponse](None, outbound).map { response =>
      if (response.error.isDefined)
        return Future.failed(throw BugsError(response.error.get.message))
      else {
        response.result.get.bugs.map(b =>
          new Bug(b.id.toString, b.severity, b.priority, b.status,
            b.resolution.getOrElse("UNRESOLVED"), b.creator, b.creation_time,
            b.assigned_to.getOrElse("UNASSIGNED"), b.last_change_time.getOrElse(b.creation_time),
            b.product.getOrElse("---"), b.component.getOrElse("---"),
            b.cf_production.getOrElse("---"), b.summary, b.platform))
      }
    }

  }
}