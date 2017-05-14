package bugapp.report.japi.model;

import bugapp.report.model.ReportValue;

import java.math.BigDecimal;

/**
 * @author Alexander Kuleshov
 */
public abstract class ReportValueBuilder {

    public static ReportValueBuilder stringValue(String value) {
        return SimpleValueBuilder.createForString(value);
    }

    public static ReportValueBuilder intValue(Integer value) {
        return SimpleValueBuilder.createForInt(value);
    }

    public static ReportValueBuilder booleanValue(Boolean value) {
        return SimpleValueBuilder.createForBoolean(value);
    }

    public static ReportValueBuilder bigdecimalValue(BigDecimal value) {
        return SimpleValueBuilder.createForBigDecimal(value);
    }

    public static ReportValueBuilder nullValue() {
        return SimpleValueBuilder.createForNull();
    }

    public static MapValueBuilder mapValue() {
        return MapValueBuilder.create();
    }

    public static ListValueBuilder listValue() {
        return ListValueBuilder.create();
    }

    public abstract ReportValue build();

}