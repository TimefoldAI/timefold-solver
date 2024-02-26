package ai.timefold.solver.core.api.solver;

/**
 * The statistics of a given problem submitted to a {@link Solver}.
 *
 * @param entityCount The number of genuine entities defined by the problem.
 * @param variableCount The number of genuine variables defined by the problem.
 * @param maximumValueRangeSize The number of possible assignments for the genuine variable with the largest range.
 * @param problemScale An approximate log of the solution space.
 */
public record ProblemStatistics(long entityCount, long variableCount, long maximumValueRangeSize, long problemScale) {
}
