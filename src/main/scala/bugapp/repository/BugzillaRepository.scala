package bugapp.repository

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import bugapp.http.HttpClient
import bugapp.{Bug, GetBugsResponse, Config, GetBugsRequest}
import de.heikoseeberger.akkahttpcirce.CirceSupport

import scala.concurrent.{ExecutionContext, Future}

class BugzillaRepository(implicit val s: ActorSystem, implicit val m: ActorMaterializer, implicit val ec: ExecutionContext) extends BugRepository with Config with CirceSupport {

  val httpClient = new HttpClient(bugzillaUrl)

  override def getBugs: Future[Seq[Bug]] = {
    import io.circe.generic.auto._

// method "Bug.search"
// params   [{"Bugzilla_login":"user","Bugzilla_password":"password","status":["RESOLVED"],"cf_target_milestone":["2016"],"cf_production":["Production"]}]
// {"error":{"message":"When using JSON-RPC over GET, you must specify a 'method' parameter. See the documentation at docs/en/html/api/Bugzilla/WebService/Server/JSONRPC.html","code":32000},"id":"http://192.168.0.2","result":null}
    val outbound = Flow[(Any, Any)].map {
      case (req, context) =>
        val httpRequest = HttpRequest(uri = "/jsonrpc.cgi", method = HttpMethods.GET)
        (httpRequest, context)
    }

    httpClient.execute[Any, GetBugsResponse](None, outbound).map{response =>
      println(response)
      List()
    }
//    httpClient.execute[Any, Seq[Post]]("/posts", None)
  }
}
