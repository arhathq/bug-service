package bugapp

import bugapp.bugzilla.{BugzillaParams, BugzillaRequest}

/**
  * @author Alexander Kuleshov
  */
class CirceJsonTests {
  import io.circe.{ Decoder, Encoder, Json => JsonC }
  import io.circe.generic.semiauto._
  import io.circe.jawn._
  import io.circe.generic.auto._, io.circe.syntax._

  //  val jsonVal = """{"service": "hello", "active": true}"""
  //  println(parse(jsonVal).getOrElse(Json.Null))

  //  val jsonErr = """{"message":"When using JSON-RPC over GET, you must specify a 'method' parameter. See the documentation at docs/en/html/api/Bugzilla/WebService/Server/JSONRPC.html","code":32000}"""
  //  var error = jawn.decode[bugapp.Error](jsonErr).valueOr(throw _)
  //  println(error)
  //
  //  val jsonBugzErr = """{"error":{"message":"When using JSON-RPC over GET, you must specify a 'method' parameter. See the documentation at docs/en/html/api/Bugzilla/WebService/Server/JSONRPC.html","code":32000},"id":"http://192.168.0.2","result":null}"""
  //  var err = jawn.decode[GetBugsResponse](jsonBugzErr).valueOr(throw _)
  //  println(err)

  //  case class Params(username: String, password: String, status: List[String], cf_target_milestone: List[String], cf_production: List[String])
  //  case class Param(key: String, value: String)
  //  object Param {
  //    implicit val encodeFoo: Encoder[Param] = deriveEncoder
  //  }

  val params = BugzillaParams.create("username", "pwd")
  val request = new BugzillaRequest("methodName", params)
  println(List(request.params).asJson.noSpaces)

  //  val p = Params("user", "qqq", List("RESOLVED","VERIFIED","CLOSED"), List("2016.1.0","2016.2.0","2016.2.0+Dev1","2016.2.0+Dev2","2016.2.0+Dev3","2016.2.0+Dev4","2016.2.0+Dev5","2016.2.1","2016.3.0"), List())
  //  println(List(p).asJson.noSpaces)

  implicit val encodeStringorList: Encoder[Either[String, List[String]]] =
    Encoder.instance(_.fold(_.asJson, _.asJson))
  @inline def encodeC[A](a: A)(implicit encode: Encoder[A]): JsonC = encode(a)
  println(encodeC(List(Map[String, Either[String, List[String]]]("a" -> Left("b"), "b" -> Left("d"), "d" -> Left("1"), "e" -> Right(List("1", "2"))))))

  //  println(Map("a" -> "1", "b" -> "2").asJson.noSpaces)
}
