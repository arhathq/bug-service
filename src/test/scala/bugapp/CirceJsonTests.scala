package bugapp

import bugapp.bugzilla.{BugzillaParams, BugzillaRequest}

/**
  * @author Alexander Kuleshov
  */
object CirceJsonTests extends App {
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

  case class Params(username: String, password: Option[String] = None, status: Option[List[String]] = None)
  object Params {
    def apply(username: String): Params = new Params(username)
  }

  val p = Params("user1")
  println(p.asJson.noSpaces)

  case class Foo(ab: Option[Int])
  val p1 = Foo(Some(1))
  println(p1.asJson.noSpaces)

  //  case class Param(key: String, value: String)
  //  object Param {
  //    implicit val encodeFoo: Encoder[Param] = deriveEncoder
  //  }

  val openBugs = BugzillaParams(
    "username1",
    "sadsf444",
    Some("2016-01-01")
  )
  println(List(openBugs).asJson.noSpaces)

  implicit val encodeStringorList: Encoder[Either[String, List[String]]] =
    Encoder.instance(_.fold(_.asJson, _.asJson))
  @inline def encodeC[A](a: A)(implicit encode: Encoder[A]): JsonC = encode(a)
  println(encodeC(List(Map[String, Either[String, List[String]]]("a" -> Left("b"), "b" -> Left("d"), "d" -> Left("1"), "e" -> Right(List("1", "2"))))))

  //  println(Map("a" -> "1", "b" -> "2").asJson.noSpaces)
}
