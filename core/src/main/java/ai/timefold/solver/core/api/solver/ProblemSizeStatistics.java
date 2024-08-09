package ai.timefold.solver.core.api.solver;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import ai.timefold.solver.core.impl.util.MathUtils;

/**
 * The statistics of a given problem submitted to a {@link Solver}.
 *
 * @param entityCount The number of genuine entities defined by the problem.
 * @param variableCount The number of genuine variables defined by the problem.
 * @param approximateValueCount The estimated number of values defined by the problem.
 *        Can be larger than the actual value count.
 * @param approximateProblemSizeLog The estimated log_10 of the problem's search space size.
 */
public record ProblemSizeStatistics(long entityCount,
        long variableCount,
        long approximateValueCount,
        double approximateProblemSizeLog) {

    private static final Locale FORMATTER_LOCALE = Locale.getDefault();
    private static final DecimalFormat BASIC_FORMATTER = new DecimalFormat("#,###");

    // Exponent should not use grouping, unlike basic
    private static final DecimalFormat EXPONENT_FORMATTER = new DecimalFormat("#");
    private static final DecimalFormat SIGNIFICANT_FIGURE_FORMATTER = new DecimalFormat("0.######");

    /**
     * Return the {@link #approximateProblemSizeLog} as a fixed point integer.
     */
    public long approximateProblemScaleLogAsFixedPointLong() {
        return Math.round(approximateProblemSizeLog * MathUtils.LOG_PRECISION);
    }

    public String approximateProblemScaleAsFormattedString() {
        return approximateProblemScaleAsFormattedString(Locale.getDefault());
    }

    String approximateProblemScaleAsFormattedString(Locale locale) {
        if (Double.isNaN(approximateProblemSizeLog) || Double.isInfinite(approximateProblemSizeLog)) {
            return "0";
        }

        if (approximateProblemSizeLog < 10) { // log_10(10_000_000_000) = 10
            return "%s".formatted(format(Math.pow(10d, approximateProblemSizeLog), BASIC_FORMATTER, locale));
        }
        // The actual number will often be too large to fit in a double, so cannot use normal
        // formatting.
        // Separate the exponent into its integral and fractional parts
        // Use the integral part as the power of 10, and the fractional part as the significant digits.
        double exponentPart = Math.floor(approximateProblemSizeLog);
        double remainderPartAsExponent = approximateProblemSizeLog - exponentPart;
        double remainderPart = Math.pow(10, remainderPartAsExponent);
        return "%s × 10^%s".formatted(
                format(remainderPart, SIGNIFICANT_FIGURE_FORMATTER, locale),
                format(exponentPart, EXPONENT_FORMATTER, locale));
    }

    /**
     * In order for tests to work currently regardless of the default system locale,
     * we need to set the locale to a known value before running the tests.
     * And because the {@link DecimalFormat} instances are initialized statically for reasons of performance,
     * we cannot expect them to be in the locale that the test expects them to be in.
     * This method exists to allow for an override.
     *
     * @param number never null
     * @param decimalFormat never null
     * @param locale never null
     * @return the given decimalFormat with the given locale
     */
    private static String format(double number, DecimalFormat decimalFormat, Locale locale) {
        if (locale.equals(FORMATTER_LOCALE)) {
            return decimalFormat.format(number);
        }
        try { // Slow path for corner cases where input locale doesn't match the default locale.
            decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(locale));
            return decimalFormat.format(number);
        } finally {
            decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(FORMATTER_LOCALE));
        }
    }

}
