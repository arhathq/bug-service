package bugapp.report.converter

import bugapp.report.model._

import scala.xml._

/**
  *
  */
class XmlReportDataConverter extends ReportDataConverter[Elem] {

  override def convert(value: ReportData): Elem = createXmlElement(value.name, resolveValues(Seq(value.fields), new NodeBuffer): _*)

  def resolveValues(values: Seq[ReportValue], acc: NodeBuffer): NodeBuffer = {
    values.foldLeft(acc) { (acc, value) =>
      value match {
        case ReportField(fieldName, fieldValue: ListValue) => acc &+ resolveFieldValue(fieldName, fieldValue)
        case ReportField(fieldName, fieldValue: MapValue) => acc &+ resolveFieldValue(fieldName, fieldValue)
        case ReportField(fieldName, fieldValue) => acc &+ createXmlElement(fieldName, resolveFieldValue(fieldName, fieldValue): _*)
        case ListValue(listValues @ _*) => resolveListValue(name = acc.last.label, listValues, acc)
        case MapValue(fields @ _*) => resolveValues(fields, acc)
        case _ => acc
      }
    }
  }

  def resolveFieldValue(field: String, value: ReportValue): NodeBuffer = value match {
    case simpleValue: SimpleValue => resolveSimpleValue(simpleValue, new NodeBuffer)
    case ListValue(listValues @ _*) => resolveListValue(field, listValues, new NodeBuffer)
    case mapValue: MapValue => resolveMapValue(field, mapValue, new NodeBuffer)
    case _ => new NodeBuffer
  }

  def resolveSimpleValue(value: SimpleValue, parent: NodeBuffer): NodeBuffer = value match {
    case IntValue(v) => parent &+ Text(v.toString)
    case StringValue(v) => parent &+ Text(v)
    case BigDecimalValue(v) => parent &+ Text(v.toString)
    case BooleanValue(v) => parent &+ Text(v.toString)
    case NullValue() => parent
  }

  def resolveListValue(name: String, values: Seq[ReportValue], parent: NodeBuffer): NodeBuffer = {
    values.foldLeft(parent) { (acc, value) =>
      value match {
        case simpleValue: SimpleValue => acc &+ createXmlElement(name, resolveSimpleValue(simpleValue, new NodeBuffer): _*)
        case ListValue(listValues @ _*) => resolveListValue(name, listValues, acc)
        case mapValue: MapValue => resolveMapValue(name, mapValue, acc)
        case ReportField(fieldName, fieldValue) => acc &+ createXmlElement(fieldName, resolveFieldValue(fieldName, fieldValue): _*)
        case _ => acc
      }
    }
  }

  def resolveMapValue(name: String, value: MapValue, parent: NodeBuffer): NodeBuffer = {
    val node = value.fields.foldLeft(new NodeBuffer) { (acc, field) =>
      acc &+ createXmlElement(field.name, resolveFieldValue(field.name, field.value): _*)
    }
    parent &+ createXmlElement(name, node: _*)
  }

  def createXmlElement(name: String, child: Node*): Elem = Elem.apply(null, name, Null, TopScope, true, child: _*)
}