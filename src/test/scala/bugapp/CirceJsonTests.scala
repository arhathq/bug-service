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
        id <- c.downField("q").as[String].right
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

    val doc: Json = parse(json).right.getOrElse(Json.Null)

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
    println(arrayQux.right.getOrElse(Array()).length)

    val arrayBugs: Decoder.Result[Array[Bar]] =
      cursor.downField("values").downField("result").downField("bugs").as[Array[Bar]]
    println(arrayBugs.right.getOrElse(Array[Bar]()).length)
  }

  test("Scala Collections to Json") {

    import io.circe.generic.semiauto._
    import io.circe.syntax._

    case class Report(name: String, fields: MapValue)

    trait ReportValue

    case class ReportField(name: String, value: ReportValue) extends ReportValue

    case class MapValue(fields: ReportField*) extends ReportValue

    trait SimpleValue extends ReportValue
    case class StringValue(value: String) extends SimpleValue
    case class IntValue(value: Int) extends SimpleValue
    case class BooleanValue(value: Boolean) extends SimpleValue
    case class BigDecimalValue(value: BigDecimal) extends SimpleValue
    case class NullValue() extends SimpleValue

    case class ListValue(values: ReportValue*) extends ReportValue

    val data = Map(
      "all-open-bugs" -> Map(
        "prioritized-bugs" -> Vector(
          Map(
            "priority" -> "P1",
            "period1" -> 0,
            "period2" -> 0,
            "period3" -> 0,
            "period4" -> 0,
            "period5" -> true,
            "period6" -> 0,
            "total" -> 0
          ),
          Map(
            "priority" -> "Grand Total",
            "period1" -> 0,
            "period2" -> 0,
            "period3" -> 0,
            "period4" -> 0,
            "period5" -> 0,
            "period6" -> 0,
            "total" -> 0
          )
        ),
        "excludedComponents" -> Vector("component1", "component2"),
        "chart-data" -> Map(

        )
      )
    )

    val report = Report("all-open-bugs",
      MapValue(
          ReportField("prioritized-bugs",
            ListValue(
                MapValue(
                    ReportField("priority", StringValue("P1")),
                    ReportField("period1", IntValue(0)),
                    ReportField("period2", IntValue(0))
                ),
                MapValue(
                    ReportField("priority", StringValue("P2")),
                    ReportField("period1", IntValue(0)),
                    ReportField("period2", IntValue(0))
                )
            )
          ),
          ReportField("excludedComponents",
            ListValue(
                StringValue("component1"),
                StringValue("component2")
            )
          ),
          ReportField("chart-data",
            MapValue()
          ),
          ReportField("stringValue", StringValue("1234")),
          ReportField("booleanValue", BooleanValue(true)),
          ReportField("bigDecimalValue", BigDecimalValue(BigDecimal(2.123456))),
          ReportField("emptyValue", NullValue())
      )
    )

    lazy implicit val reportEncoder: Encoder[Report] = Encoder.instance {
      case Report(name, fields) => JsonC.obj((name, fields.asJson))
    }
    lazy implicit val reportFieldEncoder: Encoder[ReportField] = Encoder.instance {
      case ReportField(name, value) => JsonC.obj((name, value.asJson))
    }
    lazy implicit val mapValueEncoder: Encoder[MapValue] = Encoder.instance {
      case MapValue(fields @ _*) => fields.map(field => field.name -> field.value).toMap.asJson
    }
    lazy implicit val listValueEncoder: Encoder[ListValue] = Encoder.instance {
      case ListValue(values @ _*) => values.asJson
    }
    lazy implicit val intValueEncoder: Encoder[IntValue] = Encoder.instance {
      case IntValue(value) => JsonC.fromInt(value)
    }
    lazy implicit val booleanValueEncoder: Encoder[BooleanValue] = Encoder.instance {
      case BooleanValue(value) => JsonC.fromBoolean(value)
    }
    lazy implicit val bigDecimalValueEncoder: Encoder[BigDecimalValue] = Encoder.instance {
      case BigDecimalValue(value) => JsonC.fromBigDecimal(value)
    }
    lazy implicit val nullValueEncoder: Encoder[NullValue] = Encoder.instance {
      case NullValue() => JsonC.Null
    }
    lazy implicit val stringValueEncoder = deriveEncoder[StringValue]

    lazy implicit val simpleValueEncoder: Encoder[SimpleValue] = Encoder.instance {
      case IntValue(value) => value.asJson
      case StringValue(value) => value.asJson
      case BooleanValue(value) => value.asJson
      case BigDecimalValue(value) => value.asJson
      case nil: NullValue => nil.asJson
    }

    lazy implicit val reportValueEncoder: Encoder[ReportValue] = Encoder.instance {
      case rf: ReportField => rf.asJson
      case mv: MapValue => mv.asJson
      case lv: ListValue => lv.asJson
      case sv: SimpleValue => sv.asJson
    }

    println(report.asJson)
  }
}
