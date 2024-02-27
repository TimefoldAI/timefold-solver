package ai.timefold.solver.core.api.solver;

import java.text.DecimalFormat;

import ai.timefold.solver.core.impl.util.MathUtils;

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
    public static final long LOG_SCALE = 100L;
    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("0.##");

    /**
     * Returns the approximate problem scale log in base 10.
     * Can be used to compare sizes of solution spaces.
     * 
     * @return {@link #problemScale} divided by {@link #LOG_SCALE}.
     */
    public long getUnscaledBase10Log() {
        return Math.round((problemScale / ((double) LOG_SCALE)) / MathUtils.getLogInBase(maximumValueRangeSize, 10));
    }

    public String formatApproximateProblemScale() {
        // TODO: Should this be in base 10?
        //  Using base maximumValueRangeSize will be more accurate
        //  (since most entities either have 1 possible value (pinned) or maximumValueRangeSize possible values,
        //  meaning their logs will be exactly 0 or 1 respectively).
        return "%d^{%s}".formatted(maximumValueRangeSize, DECIMAL_FORMATTER.format(problemScale / ((double) LOG_SCALE)));
    }
}
