package ai.timefold.solver.core.api.solver;

import java.util.Locale;

import ai.timefold.solver.core.impl.util.MathUtils;

import org.jspecify.annotations.NullMarked;

/**
 * The statistics of a given problem submitted to a {@link Solver}.
 *
 * @param entityCount The number of genuine entities defined by the problem.
 * @param variableCount The number of genuine variables defined by the problem.
 * @param approximateValueCount The estimated number of values defined by the problem.
 *        Can be larger than the actual value count.
 * @param approximateProblemSizeLog The estimated log_10 of the problem's search space size.
 */
@NullMarked
public record ProblemSizeStatistics(long entityCount,
        long variableCount,
        long approximateValueCount,
        double approximateProblemSizeLog) {

    /**
     * Return the {@link #approximateProblemSizeLog} as a fixed point integer.
     */
    public long approximateProblemScaleLogAsFixedPointLong() {
        return Math.round(approximateProblemSizeLog * MathUtils.LOG_PRECISION);
    }

    public String approximateProblemScaleAsFormattedString() {
        return MathUtils.approximateProblemScaleAsFormattedString(approximateProblemSizeLog, Locale.getDefault());
    }

}
