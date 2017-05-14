package bugapp.report.japi.model;

import bugapp.report.model.MapValue;
import bugapp.report.model.ReportData;

/**
 * @author Alexander Kuleshov
 */
public class ReportDataBuilder {

    private String name;
    private MapValueBuilder mapValueBuilder;

    private ReportDataBuilder(String name) {
        this.name = name;
        mapValueBuilder = MapValueBuilder.create();
    }

    public static ReportDataBuilder createFor(String name) {
        return new ReportDataBuilder(name);
    }

    public ReportDataBuilder withField(ReportFieldBuilder fieldBuilder) {
        mapValueBuilder.withField(fieldBuilder);
        return this;
    }

    public ReportData build() {
        return new ReportData(name, (MapValue) mapValueBuilder.build());
    }
}
