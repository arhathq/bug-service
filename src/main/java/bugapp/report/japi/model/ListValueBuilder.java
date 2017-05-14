package bugapp.report.japi.model;

import bugapp.report.model.ListValue;
import bugapp.report.model.ReportValue;
import scala.collection.JavaConversions;
import scala.collection.Seq;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Alexander Kuleshov
 */
public class ListValueBuilder extends ReportValueBuilder {
    private List<ReportValueBuilder> values = new ArrayList<>();

    private ListValueBuilder() {
    }

    static ListValueBuilder create() {
        return new ListValueBuilder();
    }

    public ListValueBuilder withValue(ReportValueBuilder reportField) {
        values.add(reportField);
        return this;
    }

    public ListValueBuilder withValues(List<ReportValueBuilder> reportFields) {
        values.addAll(reportFields);
        return this;
    }

    @Override
    public ReportValue build() {
        Seq<ReportValue> reportValues =
                JavaConversions.asScalaBuffer(
                        values.stream().map(ReportValueBuilder::build).collect(Collectors.toList())
                );
        return new ListValue(reportValues);
    }
}
