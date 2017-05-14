package bugapp.report.japi.model;

import bugapp.report.model.*;

import java.math.BigDecimal;

/**
 * @author Alexander Kuleshov
 */
class SimpleValueBuilder extends ReportValueBuilder {
    private Object value;
    private SimpleValueType type;

    private SimpleValueBuilder(Object value, SimpleValueType type) {
        this.value = value;
        this.type = type;
    }

    static SimpleValueBuilder createForString(String value) {
        return new SimpleValueBuilder(value, SimpleValueType.String);
    }

    static SimpleValueBuilder createForInt(Integer value) {
        return new SimpleValueBuilder(value, SimpleValueType.Integer);
    }

    static SimpleValueBuilder createForBoolean(Boolean value) {
        return new SimpleValueBuilder(value, SimpleValueType.Boolean);
    }

    static SimpleValueBuilder createForBigDecimal(BigDecimal value) {
        return new SimpleValueBuilder(value, SimpleValueType.BigDecimal);
    }

    static SimpleValueBuilder createForNull() {
        return new SimpleValueBuilder(null, SimpleValueType.Null);
    }

    @Override
    public ReportValue build() {
        switch (type) {
            case String: return new StringValue((String) value);
            case Integer: return new IntValue((Integer) value);
            case Boolean: return new BooleanValue((Boolean) value);
            case BigDecimal: return new BigDecimalValue(new scala.math.BigDecimal((BigDecimal) value));
            default: return new NullValue();
        }
    }

    private enum SimpleValueType {
        String, Integer, Boolean, BigDecimal, Null
    }
}
