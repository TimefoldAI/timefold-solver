package ai.timefold.jpyinterpreter.types.datetime;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.WeekFields;
import java.util.regex.Pattern;

import ai.timefold.jpyinterpreter.types.errors.ValueError;

/**
 * Based on the format specified
 * <a href="https://docs.python.org/3.11/library/datetime.html#strftime-and-strptime-format-codes">in
 * the datetime documentation</a>.
 */
public class PythonDateTimeFormatter {
    private final static Pattern DIRECTIVE_PATTERN = Pattern.compile("([^%]*)%(.)");

    static DateTimeFormatter getDateTimeFormatter(String pattern) {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        var matcher = DIRECTIVE_PATTERN.matcher(pattern);
        int endIndex = 0;
        while (matcher.find()) {
            var literalPart = matcher.group(1);
            builder.appendLiteral(literalPart);
            endIndex = matcher.end();

            char directive = matcher.group(2).charAt(0);
            switch (directive) {
                case 'a' -> {
                    builder.appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT);
                }
                case 'A' -> {
                    builder.appendText(ChronoField.DAY_OF_WEEK, TextStyle.FULL);
                }
                case 'w' -> {
                    builder.appendValue(ChronoField.DAY_OF_WEEK);
                }
                case 'd' -> {
                    builder.appendValue(ChronoField.DAY_OF_MONTH, 2);
                }
                case 'b' -> {
                    builder.appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT);
                }
                case 'B' -> {
                    builder.appendText(ChronoField.MONTH_OF_YEAR, TextStyle.FULL);
                }
                case 'm' -> {
                    builder.appendValue(ChronoField.MONTH_OF_YEAR, 2);
                }
                case 'y' -> {
                    builder.appendPattern("uu");
                }
                case 'Y' -> {
                    builder.appendValue(ChronoField.YEAR);
                }
                case 'H' -> {
                    builder.appendValue(ChronoField.HOUR_OF_DAY, 2);
                }
                case 'I' -> {
                    builder.appendValue(ChronoField.HOUR_OF_AMPM, 2);
                }
                case 'p' -> {
                    builder.appendText(ChronoField.AMPM_OF_DAY);
                }
                case 'M' -> {
                    builder.appendValue(ChronoField.MINUTE_OF_HOUR, 2);
                }
                case 'S' -> {
                    builder.appendValue(ChronoField.SECOND_OF_MINUTE, 2);
                }
                case 'f' -> {
                    builder.appendValue(ChronoField.MICRO_OF_SECOND, 6);
                }
                case 'z' -> {
                    builder.appendOffset("+HHmmss", "");
                }
                case 'Z' -> {
                    builder.appendZoneOrOffsetId();
                }
                case 'j' -> {
                    builder.appendValue(ChronoField.DAY_OF_YEAR, 3);
                }
                case 'U' -> {
                    builder.appendValue(WeekFields.of(DayOfWeek.SUNDAY, 7).weekOfYear(), 2);
                }
                case 'W' -> {
                    builder.appendValue(WeekFields.of(DayOfWeek.MONDAY, 7).weekOfYear(), 2);
                }
                case 'c' -> {
                    builder.appendLocalized(FormatStyle.MEDIUM, FormatStyle.MEDIUM);
                }
                case 'x' -> {
                    builder.appendLocalized(FormatStyle.MEDIUM, null);
                }
                case 'X' -> {
                    builder.appendLocalized(null, FormatStyle.MEDIUM);
                }
                case '%' -> {
                    builder.appendLiteral("%");
                }
                case 'G' -> {
                    builder.appendValue(WeekFields.of(DayOfWeek.MONDAY, 4).weekBasedYear());
                }
                case 'u' -> {
                    builder.appendValue(WeekFields.of(DayOfWeek.MONDAY, 4).dayOfWeek(), 1);
                }
                case 'V' -> {
                    builder.appendValue(WeekFields.of(DayOfWeek.MONDAY, 4).weekOfYear(), 2);
                }
                default -> {
                    throw new ValueError("Invalid directive (" + directive + ") in format string (" + pattern + ").");
                }
            }
        }
        builder.appendLiteral(pattern.substring(endIndex));
        return builder.toFormatter();
    }
}
