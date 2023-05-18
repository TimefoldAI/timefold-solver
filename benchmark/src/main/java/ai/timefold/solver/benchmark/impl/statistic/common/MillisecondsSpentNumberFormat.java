package ai.timefold.solver.benchmark.impl.statistic.common;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

import ai.timefold.solver.benchmark.impl.report.MillisecondDurationNumberFormatFactory;

public final class MillisecondsSpentNumberFormat extends NumberFormat {

    @Override
    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        return format((long) number, toAppendTo, pos);
    }

    @Override
    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
        return toAppendTo.append(MillisecondDurationNumberFormatFactory.formatMillis(number));
    }

    @Override
    public Number parse(String source, ParsePosition parsePosition) {
        throw new UnsupportedOperationException();
    }

}
