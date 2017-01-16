package bugapp

import bugapp.report.converter.{JsonReportDataConverter, XmlReportDataConverter}
import bugapp.report.model._
import org.scalatest.FunSuite

/**
  *
  */
class ReportDataModelTests extends FunSuite {

  val report = ReportData("all-open-bugs",
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
        MapValue(
          ReportField("dataSet1",
            ListValue(
              ReportField("x", IntValue(1)),
              ReportField("y", IntValue(0)),
              ReportField("legend", StringValue("weeks")),
              ReportField("element",
                ListValue(
                  IntValue(1), IntValue(2), IntValue(3)
                )
              )
            )
          ),
          ReportField("dataSet2",
            ListValue(
              ReportField("x", IntValue(12)),
              ReportField("y", IntValue(3)),
              ReportField("legend", StringValue("weeks")),
              MapValue(
                ReportField("notes", StringValue("Some notes"))
              )
            )
          )
        )
      ),
      ReportField("stringValue", StringValue("1234")),
      ReportField("booleanValue", BooleanValue(true)),
      ReportField("bigDecimalValue", BigDecimalValue(BigDecimal(2.123456))),
      ReportField("emptyValue", NullValue())
    )
  )


  test("Report Data Model to Json") {
    val converter = new JsonReportDataConverter
    val json = converter.convert(report)

    println(s"Report as JSON:\n $json")
  }

  test("Report Data Model to XML") {
    val converter = new XmlReportDataConverter
    val xml = converter.convert(report)

    println(s"Report as XML:\n $xml")
  }

}
