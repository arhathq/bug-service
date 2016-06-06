package bugapp.repository

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import bugapp.http.HttpClient
import bugapp._
import de.heikoseeberger.akkahttpcirce.CirceSupport

import scala.concurrent.{ExecutionContext, Future}

class BugzillaRepository(implicit val s: ActorSystem, implicit val m: ActorMaterializer, implicit val ec: ExecutionContext) extends BugRepository with Config with CirceSupport {

  val httpClient = new HttpClient(bugzillaUrl)

  override def getBugs: Future[Seq[Bug]] = {
// method "Bug.search"
// params   [{"Bugzilla_login":"user","Bugzilla_password":"password","status":["RESOLVED"],"cf_target_milestone":["2016"],"cf_production":["Production"]}]
// {"error":{"message":"When using JSON-RPC over GET, you must specify a 'method' parameter. See the documentation at docs/en/html/api/Bugzilla/WebService/Server/JSONRPC.html","code":32000},"id":"http://192.168.0.2","result":null}
    getBugs(GetBugsRequest(bugzillaUsername, bugzillaPassword))
  }

  private def getBugs(request: GetBugsRequest): Future[Seq[Bug]] = {
    import io.circe.generic.auto._

    val getBugsUri = Uri(bugzillaUrl).
      withPath(Uri.Path("/jsonrpc.cgi")).
      withQuery(Uri.Query(
        Map(
          "method" -> request.method,
          "params" -> """[{"Bugzilla_login":"","Bugzilla_password":"","status":["RESOLVED","VERIFIED","CLOSED"]}]"""
        )
      ))

    val outbound = Flow[(Any, Any)].map {
      case (req, context) =>
        val httpRequest = HttpRequest(uri = getBugsUri, method = HttpMethods.GET)
        (httpRequest, context)
    }

    httpClient.execute[Any, GetBugsResponse](None, outbound).map {response =>
      println(response)
      if (response.error.isDefined)
        return Future.failed(new BugsError(response.error.get.message))
      else {
        response.result.get.bugs
      }
    }

  }
}

object CirceJ extends App {
  import io.circe.{ Decoder, Encoder, Json => JsonC }
  import io.circe.generic.semiauto._
  import io.circe.jawn._

  //  val jsonVal = """{"service": "hello", "active": true}"""
//  println(parse(jsonVal).getOrElse(Json.Null))

//  val jsonErr = """{"message":"When using JSON-RPC over GET, you must specify a 'method' parameter. See the documentation at docs/en/html/api/Bugzilla/WebService/Server/JSONRPC.html","code":32000}"""
//  var error = jawn.decode[bugapp.Error](jsonErr).valueOr(throw _)
//  println(error)
//
//  val jsonBugzErr = """{"error":{"message":"When using JSON-RPC over GET, you must specify a 'method' parameter. See the documentation at docs/en/html/api/Bugzilla/WebService/Server/JSONRPC.html","code":32000},"id":"http://192.168.0.2","result":null}"""
//  var err = jawn.decode[GetBugsResponse](jsonBugzErr).valueOr(throw _)
//  println(err)

  case class Params(username: String, password: String, status: List[String], cf_target_milestone: List[String], cf_production: List[String])
  case class Param(key: String, value: String)
  object Param {
    implicit val encodeFoo: Encoder[Param] = deriveEncoder
  }


  val p = Params("user", "qqq", List("RESOLVED","VERIFIED","CLOSED"), List("2016.1.0","2016.2.0","2016.2.0+Dev1","2016.2.0+Dev2","2016.2.0+Dev3","2016.2.0+Dev4","2016.2.0+Dev5","2016.2.1","2016.3.0"), List())

//  println(List(p).asJson.noSpaces)

  @inline def encodeC[A](a: A)(implicit encode: Encoder[A]): JsonC = encode(a)

//  implicit val encoder: Encoder[Map[String, Param]] = deriveEncoder

  println(encodeC(Map[String, String]("a" -> "b", "b" -> "d", "d" -> "1")))

//  println(Map("a" -> "1", "b" -> "2").asJson.noSpaces)
}