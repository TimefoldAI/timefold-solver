package ai.timefold.solver.core.api.solver;

import java.time.Duration;

import ai.timefold.solver.core.api.solver.change.ProblemChange;

import org.jspecify.annotations.NullMarked;

/**
 * Statistics of a finished solver run.
 * <p>
 * The counts, speeds and duration are totals for the whole run.
 * After a {@link ProblemChange problem change}, the solver restarts,
 * and these totals include every segment before and after each restart.
 * The live getters on {@link SolverJob} only cover the segment since the last problem change,
 * so their values can be smaller.
 */
@NullMarked
public interface SolverRunSummary {

    /**
     * @return the sum of the time spent in the entire run;
     *         if the run included {@link ProblemChange problem changes},
     *         the time taken to process them is not included here.
     */
    Duration solvingDuration();

    /**
     * @return at least 0, the number of score calculations in the whole run
     */
    long scoreCalculationCount();

    /**
     * @return at least 0, {@link #scoreCalculationCount()} over {@link #solvingDuration()};
     *         scores per second.
     */
    long scoreCalculationSpeed();

    /**
     * @return at least 0, the number of move evaluations in the whole run
     */
    long moveEvaluationCount();

    /**
     * @return at least 0, move evaluations per second over {@link #solvingDuration()}
     */
    long moveEvaluationSpeed();

    /**
     * @return true if every planning variable of the final best solution is assigned
     */
    boolean isSolutionInitialized();

    /**
     * @return the size statistics of the problem in its final state
     */
    ProblemSizeStatistics problemSizeStatistics();

}
