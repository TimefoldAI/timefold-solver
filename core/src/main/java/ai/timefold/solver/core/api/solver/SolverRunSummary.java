package ai.timefold.solver.core.api.solver;

import java.time.Duration;

import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.api.solver.phase.PhaseId;
import ai.timefold.solver.core.api.solver.termination.TerminationId;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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

    /**
     * The run terminated early after {@link Solver#terminateEarly()}, {@link SolverJob#terminateEarly()},
     * {@link SolverManager#terminateEarly(Object)}, or an interrupt of the solver thread.
     * A daemon solver ends only this way, so for a daemon solver this is always true.
     *
     * @return true if the run terminated early
     */
    boolean isTerminatedEarly();

    /**
     * The termination that ended the run:
     * a termination configured on the solver,
     * or a termination configured on the phase that ran last.
     * A termination of an earlier phase is never reported,
     * because the next phase starts after it.
     * If the last phase is a Partitioned Search phase,
     * a termination configured on that phase is not reported, because only the partitions evaluate it.
     * See {@link TerminationId} for the id model.
     * <p>
     * If this is null and {@link #isTerminatedEarly()} is false,
     * the {@link #lastPhase() last phase} completed.
     *
     * @return null if {@link #isTerminatedEarly()} is true, or if the last phase completed
     */
    @Nullable
    TerminationId termination();

    /**
     * @return the last phase that started; null if no phase ran
     */
    @Nullable
    PhaseId lastPhase();

    /**
     * The time from the start of the run until the final best solution was set,
     * summed over the segments of the run like {@link #solvingDuration()}.
     * It is about zero if the solver did not improve the input solution.
     * After a {@link ProblemChange problem change}, the solver restarts and sets a new best solution.
     *
     * @return {@link Duration#ZERO} if there is no best solution
     */
    Duration timeToBestSolution();

}
