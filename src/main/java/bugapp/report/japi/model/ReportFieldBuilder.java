package bugapp.report.japi.model;

import bugapp.report.model.ReportField;

/**
 * @author Alexander Kuleshov
 */
public class ReportFieldBuilder {
    private String name;
    private ReportValueBuilder value;

    private ReportFieldBuilder(String name) {
        this.name = name;
    }

    public static ReportFieldBuilder createFor(String name) {
        return new ReportFieldBuilder(name);
    }

    public ReportFieldBuilder withValue(ReportValueBuilder reportValue) {
        this.value = reportValue;
        return this;
    }

    public ReportField build() {
        return new ReportField(name, value.build());
    }
}
