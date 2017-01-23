package bugapp

import bugapp.report.converter.{JsonReportDataConverter, XmlReportDataConverter}
import bugapp.report.model._
import org.scalatest.FunSuite

/**
  *
  */
class ReportDataModelTests extends FunSuite {

  val jsonConverter = new JsonReportDataConverter()
  val xmlConverter = new XmlReportDataConverter()
  val xmlPrinter = new scala.xml.PrettyPrinter(80, 4)

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
              MapValue(
                ReportField("x", IntValue(1)),
                ReportField("y", IntValue(0)),
                ReportField("legend", StringValue("weeks")),
                ReportField("element",
                  ListValue(
                    IntValue(1), IntValue(2), IntValue(3)
                  )
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
    val p = new scala.xml.PrettyPrinter(80, 4)
    println(s"Report as XML:\n ${p.format(xml)}")
  }

  test("Week Summary Report") {
    val data =
    ReportData("week-summary-report",
      MapValue(
        ReportField("production-queue",
          MapValue(
            ReportField("state",StringValue("changed")),
            ReportField("from",IntValue(0)),
            ReportField("to",IntValue(134)),
            ReportField("high-priotity-bugs",IntValue(11)),
            ReportField("blocked-bugs",IntValue(39))
          )
        ),
        ReportField("statistics",
          MapValue(
            ReportField("new", IntValue(29)),
            ReportField("reopened", IntValue(2)),
            ReportField("moved", IntValue(65)),
            ReportField("resolved", IntValue(34)),
            ReportField("bugs-updated", IntValue(71)),
            ReportField("total-comments",IntValue(232))
          )
        ),
        ReportField("bugs-count",
          MapValue(
            ReportField("period", IntValue(15)),
            ReportField("table",
              ReportField("row",
                ListValue(
                  MapValue(
                    ReportField("line", StringValue("Mark")),
                    ReportField("invalid", BigDecimalValue(5.0)),
                    ReportField("closed", BigDecimalValue(12.0)),
                    ReportField("open", BigDecimalValue(12.0)),
                    ReportField("total", BigDecimalValue(29.0))
                  ),
                  MapValue(
                    ReportField("line", StringValue("Average")),
                    ReportField("invalid", BigDecimalValue(34.6)),
                    ReportField("closed", BigDecimalValue(31.666666666666668)),
                    ReportField("open", BigDecimalValue(9.0)),
                    ReportField("total", BigDecimalValue(22.79310344827586))
                  ),
                  MapValue(
                    ReportField("line", StringValue("Total")),
                    ReportField("invalid", BigDecimalValue(173.0)),
                    ReportField("closed", BigDecimalValue(380.0)),
                    ReportField("open", BigDecimalValue(108.0)),
                    ReportField("total", BigDecimalValue(661.0))
                  )
                )
              )
            )
          )
        )
      )
    )


    println(xmlPrinter.format(xmlConverter.convert(data)))
    println(jsonConverter.convert(data))


    val excludedComponents = Seq("Component1", "Component2")
    val notes = s"* Bugs ${excludedComponents.mkString("\"", "\", \"", "\"")} excluded from report"
    val full =
      ReportData("bug-reports",
        MapValue(
          Seq (
            ReportField("report-header",
              ReportField("date", StringValue("2017-01-19T11:38:31.718+03:00"))
            ),
            ReportField("report-footer",
              ReportField("note", StringValue(notes))
            )
          ) ++ Seq(data).map(data => ReportField(data.name, data.fields)): _*
        )
      )

    println(xmlPrinter.format(xmlConverter.convert(full)))
    println(jsonConverter.convert(full))

  }

  test("BugsByPeriodChart Report") {

    val data =
    ReportData("bugs-by-weeks-15",
      MapValue(
        ReportField("weekly-bugs",
          MapValue(
            ReportField("header",
              MapValue(
                ReportField("w2016-40",StringValue("2016-40")),
                ReportField("w2016-41",StringValue("2016-41")),
                ReportField("w2016-42",StringValue("2016-42"))
              )
            ),
            ReportField("row",
              ListValue(
                MapValue(
                  ReportField("name",StringValue("Closed")),
                  ReportField("value",
                    ListValue(
                      StringValue("2"), StringValue("26"), StringValue("30")
                    )
                  )
                ),
                MapValue(
                  ReportField("name",StringValue("Open")),
                  ReportField("value",
                    ListValue(
                      StringValue("1"), StringValue("7"), StringValue("3")
                    )
                  )
                ),
                MapValue(
                  ReportField("name",StringValue("Invalid")),
                  ReportField("value",
                    ListValue(
                      StringValue("1"), StringValue("7"), StringValue("3")
                    )
                  )
                )
              )
            )
          )
        )
      )
    )

    println(xmlPrinter.format(xmlConverter.convert(data)))

  }
}
