package ai.timefold.solver.core.api.solver;

import java.text.DecimalFormat;

import ai.timefold.solver.core.impl.util.MathUtils;

/**
 * The statistics of a given problem submitted to a {@link Solver}.
 *
 * @param entityCount The number of genuine entities defined by the problem.
 * @param variableCount The number of genuine variables defined by the problem.
 * @param approximateValueCount The estimated number of values defined by the problem.
 *        Can be larger than the actual value count.
 * @param approximateProblemSizeLog The estimated log_10 of the problem's search space.
 */
public record ProblemSizeStatistics(long entityCount,
        long variableCount,
        long approximateValueCount,
        double approximateProblemSizeLog) {

    private static final DecimalFormat BASIC_FORMATTER = new DecimalFormat("#,###");

    // Exponent should not use grouping, unlike basic
    private static final DecimalFormat EXPONENT_FORMATTER = new DecimalFormat("#");
    private static final DecimalFormat SIGNIFICANT_FIGURE_FORMATTER = new DecimalFormat("0.######");

    /**
     * Return the {@link #approximateProblemSizeLog} as a fixed point integer.
     */
    public long getApproximateProblemScaleLogAsFixedPointLong() {
        return Math.round(approximateProblemSizeLog * MathUtils.LOG_PRECISION);
    }

    public String formatApproximateProblemScale() {
        if (approximateProblemSizeLog < 10) {
            // log_10(10_000_000_000) = 10
            return "~%s".formatted(BASIC_FORMATTER.format(Math.pow(10d, approximateProblemSizeLog)));
        }
        // The actual number will often be too large to fit in a double, so cannot use normal
        // formatting.
        // Separate the exponent into its integral and fractional parts
        // Use the integral part as the power of 10, and the fractional part as the significant digits.
        double exponentPart = Math.floor(approximateProblemSizeLog);
        double remainderPartAsExponent = approximateProblemSizeLog - exponentPart;
        double remainderPart = Math.pow(10, remainderPartAsExponent);
        return "~%se%s".formatted(
                SIGNIFICANT_FIGURE_FORMATTER.format(remainderPart),
                EXPONENT_FORMATTER.format(exponentPart));
    }
}
