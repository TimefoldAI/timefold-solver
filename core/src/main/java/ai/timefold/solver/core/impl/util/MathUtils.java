package ai.timefold.solver.core.impl.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Includes code taken from Apache's commons-math library, v3.6.1 (Apache 2.0-licensed)
 * These methods are clearly marked in comments, their modifications listed.
 */
public class MathUtils {

    public static final long LOG_PRECISION = 1_000_000L;
    private static final int FACTORIAL_MAX_N = 20;
    private static final long[] FACTORIAL_CACHE = new long[FACTORIAL_MAX_N - 1]; // 0 and 1 are hard-coded.
    public static final Locale FORMATTER_LOCALE = Locale.getDefault();
    private static final DecimalFormat BASIC_FORMATTER = new DecimalFormat("#,###");
    // Exponent should not use grouping, unlike basic
    private static final DecimalFormat EXPONENT_FORMATTER = new DecimalFormat("#");
    private static final DecimalFormat SIGNIFICANT_FIGURE_FORMATTER = new DecimalFormat("0.######");

    private MathUtils() {
    }

    public static long getPossibleArrangementsScaledApproximateLog(long scale, long base, int listSize, int partitions) {
        double result;
        if (listSize == 0 || partitions == 0) {
            // Only one way to divide an empty list, and the log of 1 is 0
            // Likewise, there is only 1 way to divide a list into 0 partitions
            // (since it impossible to do)
            result = 0L;
        } else if (partitions <= 2) {
            // If it a single partition, it the same as the number of permutations.
            // If it two partitions, it the same as the number of permutations of a list of size
            // n + 1 (where we add an element to seperate the two partitions)
            result = factorialLog(listSize + partitions - 1);
        } else {
            // If it n > 2 partitions, (listSize + partitions - 1)! will overcount by
            // a multiple of (partitions - 1)!
            result = factorialLog(listSize + partitions - 1) - factorialLog(partitions - 1);
        }

        // Need to change base to use the given base
        return Math.round(scale * result / Math.log(base));
    }

    /**
     * Returns a scaled approximation of a log
     *
     * @param scale What to scale the result by. Typically, a power of 10.
     * @param base The base of the log
     * @param value The parameter to the log function
     * @return A value approximately equal to {@code scale * log_base(value)}, rounded
     *         to the nearest integer.
     */
    public static long getScaledApproximateLog(long scale, long base, long value) {
        return Math.round(scale * getLogInBase(base, value));
    }

    /**
     * Returns a scaled approximation of a log.
     *
     * @param scale What to scale the result by. Typically, a power of 10.
     * @param base The base of the log
     * @param value The parameter to the log function
     * @return A value approximately equal to {@code scale * log_base(value)}, rounded to the nearest integer.
     */
    public static long getScaledApproximateLog(long scale, long base, double value) {
        return Math.round(scale * getLogInBase(base, value));
    }

    public static double getLogInBase(double base, double value) {
        return Math.log(value) / Math.log(base);
    }

    public static long getSpeed(long count, long timeMillisSpent) {
        // Avoid divide by zero exception on a fast CPU
        return count * 1000L / Math.max(timeMillisSpent, 1L);
    }

    static long factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Factorial of %d is undefined."
                    .formatted(n));
        } else if (n > FACTORIAL_MAX_N) {
            throw new IllegalArgumentException("Factorial of %d is too large to fit in a long."
                    .formatted(n));
        }
        return switch (n) {
            case 0, 1 -> 1;
            default -> {
                var factorialCacheIndex = n - 2; // n={0,1} are already handled above.
                var factorial = FACTORIAL_CACHE[factorialCacheIndex];
                if (factorial != 0L) {
                    yield factorial;
                }
                yield FACTORIAL_CACHE[factorialCacheIndex] = n * factorial(n - 1);
            }
        };
    }

    private static double factorialLog(int n) {
        if (n <= FACTORIAL_MAX_N) {
            return Math.log(factorial(n));
        }
        var logSum = 0.0d;
        for (var i = 2; i <= n; i++) {
            logSum += Math.log(i);
        }
        return logSum;
    }

    public static String approximateProblemScaleAsFormattedString(double approximateProblemSizeLog, Locale locale) {
        if (Double.isNaN(approximateProblemSizeLog) || Double.isInfinite(approximateProblemSizeLog)) {
            return "0";
        }

        if (approximateProblemSizeLog < 10) { // log_10(10_000_000_000) = 10
            return "%s".formatted(formatNumber(Math.pow(10d, approximateProblemSizeLog), BASIC_FORMATTER, locale));
        }
        // The actual number will often be too large to fit in a double, so cannot use basic
        // formatting.
        // Separate the exponent into its integral and fractional parts
        // Use the integral part as the power of 10, and the fractional part as the significant digits.
        double exponentPart = Math.floor(approximateProblemSizeLog);
        double remainderPartAsExponent = approximateProblemSizeLog - exponentPart;
        double remainderPart = Math.pow(10, remainderPartAsExponent);
        return "%s × 10^%s".formatted(
                formatNumber(remainderPart, SIGNIFICANT_FIGURE_FORMATTER, locale),
                formatNumber(exponentPart, EXPONENT_FORMATTER, locale));
    }

    /**
     * In order for tests to work currently regardless of the default system locale,
     * we need to set the locale to a known value before running the tests.
     * And because the {@link DecimalFormat} instances are initialized statically for reasons of performance,
     * we cannot expect them to be in the locale that the test expects them to be in.
     * This method exists to allow for an override.
     *
     * @return the given decimalFormat with the given locale
     */
    private static String formatNumber(double number, DecimalFormat decimalFormat, Locale locale) {
        if (locale.equals(MathUtils.FORMATTER_LOCALE)) {
            return decimalFormat.format(number);
        }
        try { // Slow path for corner cases where input locale doesn't match the default locale.
            decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(locale));
            return decimalFormat.format(number);
        } finally {
            decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(MathUtils.FORMATTER_LOCALE));
        }
    }

}
