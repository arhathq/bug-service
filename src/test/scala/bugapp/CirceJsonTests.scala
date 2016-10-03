package bugapp

import org.scalatest.FunSuite
import io.circe.{Decoder, Encoder, Printer, Json => JsonC}

/**
  * @author Alexander Kuleshov
  */
class CirceJsonTests extends FunSuite {
  import io.circe.generic.auto._, io.circe.syntax._
  import Implicits._

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

    def apply(username: String, password: String): Params = new Params(username, Some(password), None)

    def jsonPrinter(): Printer = Printer(preserveOrder = true, dropNullKeys = true, indent = "")
  }

  case class Bar(i: String)

  object Bar {
    implicit val decodeB: Decoder[Bar] = Decoder.instance(c =>
      for {
        id <- c.downField("q").as[String]
      } yield Bar(id)
    )
  }

  test("Params test") {
    val p = Params("user1", "pwd")
    println(p.asJson.pretty(Params.jsonPrinter()))
  }

  test("Foo") {
    case class Foo(ab: Option[Int])
    val p1 = Foo(Some(1))
    println(p1.asJson.spaces2)
  }

  test("Implicit encoder") {
    implicit val encodeStringorList: Encoder[Either[String, List[String]]] =
      Encoder.instance(_.fold(_.asJson, _.asJson))

    @inline def encodeC[A](a: A)(implicit encode: Encoder[A]): JsonC = encode(a)

    println(encodeC(List(Map[String, Either[String, List[String]]]("a" -> Left("b"), "b" -> Left("d"), "d" -> Left("1"), "e" -> Right(List("1", "2"))))))

    println(Map("a" -> "1", "b" -> "2").asJson.noSpaces)
  }

  test("Iterating through Json") {
    import io.circe._, io.circe.parser._

    val json: String =
      """
      {
        "id": "c730433b-082c-4984-9d66-855c243266f0",
        "name": "Foo",
        "counts": [1, 2, 3],
        "values": {
          "bar": true,
          "baz": 100.001,
          "qux": ["a", "b"],
          "result" : {
            "bugs" : [
              {"q" : "1"}, {"q" : "2"}, {"q" : "3"}
            ]
          }
        }
      }"""

    val doc: Json = parse(json).getOrElse(Json.Null)

    val cursor: HCursor = doc.hcursor

    val baz: Decoder.Result[Double] =
      cursor.downField("values").downField("baz").as[Double]
    println(baz)

    // You can also use `get[A](key)` as shorthand for `downField(key).as[A]`
    val baz2: Decoder.Result[Double] =
      cursor.downField("values").get[Double]("baz")

    val secondQux: Decoder.Result[String] =
      cursor.downField("values").downField("qux").downArray.right.as[String]
    println(secondQux)

    val arrayQux: Decoder.Result[Array[String]] =
      cursor.downField("values").downField("qux").as[Array[String]]
    println(arrayQux.getOrElse(Array()).length)

    val arrayBugs: Decoder.Result[Array[Bar]] =
      cursor.downField("values").downField("result").downField("bugs").as[Array[Bar]]
    println(arrayBugs.getOrElse(Array[Bar]()).length)
  }

}
