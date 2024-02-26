package ai.timefold.solver.core.api.solver;

import java.text.DecimalFormat;

/**
 * The statistics of a given problem submitted to a {@link Solver}.
 *
 * @param entityCount The number of genuine entities defined by the problem.
 * @param variableCount The number of genuine variables defined by the problem.
 * @param maximumValueRangeSize The number of possible assignments for the genuine variable with the largest range.
 * @param problemScale A scaled log on the approximation of the problem's scale.
 */
public record ProblemStatistics(long entityCount, long variableCount, long maximumValueRangeSize, long problemScale) {

    /**
     * The scale used on {@link #problemScale()}
     */
    public final static long LOG_SCALE = 100L;
    private static DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("0.##");

    /**
     * Returns the unscaled problem scale
     * 
     * @return {@link #problemScale} divided by {@link #LOG_SCALE}.
     */
    public long getUnscaledProblemScaleLog() {
        return problemScale / LOG_SCALE;
    }

    public String formatApproximateProblemScale() {
        return "%d^{%s}".formatted(maximumValueRangeSize, DECIMAL_FORMATTER.format(problemScale / ((double) LOG_SCALE)));
    }
}
