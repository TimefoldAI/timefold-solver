package ai.timefold.solver.core.api.solver;

import java.time.Duration;

import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.api.solver.phase.PhaseId;
import ai.timefold.solver.core.api.solver.termination.TerminationId;
import ai.timefold.solver.core.api.solver.termination.TerminationType;

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
     * @return true if every planning variable of the final best solution is assigned;
     *         when the domain allows unassigned variables/values,
     *         this is effectively always true because null values are allowed.
     */
    boolean solutionInitialized();

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
    boolean terminatedEarly();

    /**
     * The termination that ended the run:
     * a termination configured on the solver,
     * or a termination configured on the phase that ran last.
     * A termination of an earlier phase is never reported,
     * because the next phase starts after it.
     * If the last phase is a Partitioned Search phase,
     * a termination configured on that phase is not reported, because only the partitions evaluate it.
     * <p>
     * In an {@code OR} element of the termination configuration,
     * the reported termination is the first of its terminations that is true.
     * An {@code AND} element with two or more terminations is reported as itself;
     * with only one termination, that termination is reported.
     * Thus an id of type {@link TerminationType#OR} is never returned.
     * For example, with this solver termination configuration:
     *
     * <pre>{@code
     * <termination>
     *   <secondsSpentLimit>30</secondsSpentLimit>                  <!-- returned as type SPENT_LIMIT -->
     *   <termination>
     *     <terminationCompositionStyle>AND</terminationCompositionStyle>
     *     <stepCountLimit>100</stepCountLimit>
     *     <unimprovedStepCountLimit>10</unimprovedStepCountLimit>
     *   </termination>                                             <!-- returned as type AND, when both are true -->
     * </termination>
     * }</pre>
     *
     * this returns either the {@link TerminationType#SPENT_LIMIT} termination,
     * or the nested {@link TerminationType#AND} element; never the outer {@code OR} element,
     * and never {@link TerminationType#STEP_COUNT_LIMIT} or {@link TerminationType#UNIMPROVED_STEP_COUNT_LIMIT} alone.
     * <p>
     * If this is null and {@link #terminatedEarly()} is false,
     * the {@link #lastPhase() last phase} completed.
     *
     * @return null if {@link #terminatedEarly()} is true, or if the last phase completed
     */
    @Nullable
    TerminationId termination();

    /**
     * The last phase that started, whether it completed or a termination ended it.
     * <ul>
     * <li>If a termination configured on the solver ends the run before the next phase starts,
     * the phase that was last started is reported.
     * This is the phase that was either running at the time of solver termination,
     * or has completed recently without the next phase starting yet.</li>
     * <li>For a Partitioned Search phase, this is the Partitioned Search phase itself,
     * never a phase that runs inside a partition.</li>
     * <li>A Ruin and Recreate move runs a Construction Heuristic inside a Local Search step.
     * That Construction Heuristic is never reported; the Local Search phase is.</li>
     * <li>After a problem change restarts the solver,
     * this is the last phase that started after the restart.</li>
     * </ul>
     *
     * @return null if no phase ran, for example because no planning entity is movable
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
