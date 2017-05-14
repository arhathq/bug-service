package bugapp.report.japi.model;

import bugapp.report.model.ReportData;
import junit.framework.TestCase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexander Kuleshov
 */

public class ReportDataBuilderTest extends TestCase {
    public void testReportData() {

        List<ReportValueBuilder> accounts = new ArrayList<>();
        accounts.add(ReportValueBuilder.intValue(617513139));
        accounts.add(ReportValueBuilder.intValue(420616237));

        List<ReportFieldBuilder> customer = new ArrayList<>();
        customer.add(ReportFieldBuilder.createFor("firstname").withValue(ReportValueBuilder.stringValue("John")));
        customer.add(ReportFieldBuilder.createFor("lastname").withValue(ReportValueBuilder.stringValue("Doe")));
        customer.add(ReportFieldBuilder.createFor("accountNumbers").withValue(ReportValueBuilder.listValue().
                withValues(accounts)
        ));

        ReportData data = ReportDataBuilder.createFor("empty-bugs").
                withField(ReportFieldBuilder.createFor("param1").withValue(ReportValueBuilder.stringValue("value1"))).
                withField(ReportFieldBuilder.createFor("param2").withValue(ReportValueBuilder.intValue(1))).
                withField(ReportFieldBuilder.createFor("param3").withValue(ReportValueBuilder.booleanValue(false))).
                withField(ReportFieldBuilder.createFor("param4").withValue(ReportValueBuilder.bigdecimalValue(new BigDecimal(32.45)))).
                withField(ReportFieldBuilder.createFor("nullParam").withValue(ReportValueBuilder.nullValue())).
                withField(ReportFieldBuilder.createFor("listParam").withValue(
                        ReportValueBuilder.listValue().
                                withValue(ReportValueBuilder.intValue(2)).
                                withValue(ReportValueBuilder.intValue(3)).
                                withValue(ReportValueBuilder.intValue(7))
                )).
                withField(ReportFieldBuilder.createFor("customer").withValue(
                        ReportValueBuilder.mapValue().withFields(customer)
                )).
                build();

        assertEquals(data.name(), "empty-bugs");
    }
}
