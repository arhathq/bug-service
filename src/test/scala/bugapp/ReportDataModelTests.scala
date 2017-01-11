package bugapp

import org.scalatest.FunSuite
import io.circe.{Encoder, Json => JsonC}
import io.circe.syntax._

/**
  *
  */
class ReportDataModelTests extends FunSuite {

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
  lazy implicit val stringValueEncoder: Encoder[StringValue] = Encoder.instance {
    case StringValue(value) => JsonC.fromString(value)
  }

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


  test("Report Data Model to Json") {

    println(report.asJson)

  }

  test("Report Data Model to XML") {

    iterate(report)

    def iterate(report: Report) = {
      val values = report.fields.fields.asInstanceOf[Seq[ReportValue]]

      println(s"Number of simple values: ${reqIterate(values, 0)}")

      def reqIterate(values: Seq[ReportValue], acc: Int): Int = {
        values.foldLeft(acc) { (acc, value) =>
          value match {
            case _: SimpleValue => acc + 1
            case ReportField(_, value1) => reqIterate(Seq(value1), acc)
            case ListValue(values1 @ _*) => reqIterate(values1, acc)
            case MapValue(fields @ _*) => reqIterate(fields, acc)
          }

        }
      }
    }

  }

}
