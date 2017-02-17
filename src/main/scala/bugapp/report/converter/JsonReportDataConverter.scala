package bugapp.report.converter

import bugapp.report.model._
import io.circe.{Encoder, Json => JsonC}
import io.circe.syntax._


/**
  *
  */
class JsonReportDataConverter extends ReportDataConverter[JsonC] {
  import JsonReportDataConverter._

  override def convert(value: ReportData): JsonC = value.asJson
}

object JsonReportDataConverter {

  lazy implicit val reportEncoder: Encoder[ReportData] = Encoder.instance {
    case ReportData(name, fields) => JsonC.obj((name, fields.asJson))
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

}
