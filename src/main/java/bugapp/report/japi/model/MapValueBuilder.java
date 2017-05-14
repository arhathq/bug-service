package bugapp.report.japi.model;

import bugapp.report.model.MapValue;
import bugapp.report.model.ReportField;
import bugapp.report.model.ReportValue;
import scala.collection.JavaConversions;
import scala.collection.Seq;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Alexander Kuleshov
 */
public class MapValueBuilder extends ReportValueBuilder {
    private List<ReportFieldBuilder> fields = new ArrayList<>();

    private MapValueBuilder() {
    }

    static MapValueBuilder create() {
        return new MapValueBuilder();
    }

    public MapValueBuilder withField(ReportFieldBuilder reportField) {
        fields.add(reportField);
        return this;
    }

    public MapValueBuilder withFields(List<ReportFieldBuilder> reportFields) {
        fields.addAll(reportFields);
        return this;
    }

    @Override
    public ReportValue build() {
        Seq<ReportField> reportFields =
                JavaConversions.asScalaBuffer(
                        fields.stream().map(ReportFieldBuilder::build).collect(Collectors.toList())
                );
        return new MapValue(reportFields);
    }
}
