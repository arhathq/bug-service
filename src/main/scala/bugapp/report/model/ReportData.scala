package bugapp.report.model

/**
  *
  */
case class ReportData(name: String, fields: MapValue)

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