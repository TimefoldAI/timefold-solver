package ai.timefold.solver.core.api.solver;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.solver.change.ProblemChange;

import org.jspecify.annotations.NullMarked;

/**
 * Represents a {@link PlanningSolution problem} that has been submitted to solve on the {@link SolverManager}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public interface SolverJob<Solution_> {

    /**
     * @return a value given to {@link SolverManager#solve(Object, Object, Consumer)}
     *         or {@link SolverManager#solveAndListen(Object, Object, Consumer)}
     */
    Object getProblemId();

    /**
     * Returns whether the {@link Solver} is scheduled to solve, actively solving or not.
     * <p>
     * Returns {@link SolverStatus#NOT_SOLVING} if the solver already terminated.
     *
     */
    SolverStatus getSolverStatus();

    /**
     * As defined by {@link #addProblemChanges(List)}, only for a single problem change.
     * Prefer to submit multiple {@link ProblemChange}s at once to reduce the considerable overhead of multiple calls.
     */
    default CompletableFuture<Void> addProblemChange(ProblemChange<Solution_> problemChange) {
        return addProblemChanges(Collections.singletonList(problemChange));
    }

    /**
     * Schedules a batch of {@link ProblemChange problem changes} to be processed
     * by the underlying {@link Solver} and returns immediately.
     *
     * @param problemChangeList at least one problem change to be processed
     * @return completes after the best solution containing this change has been consumed.
     * @throws IllegalStateException if the underlying {@link Solver} is not in the {@link SolverStatus#SOLVING_ACTIVE}
     *         state
     * @see ProblemChange Learn more about problem change semantics.
     */
    CompletableFuture<Void> addProblemChanges(List<ProblemChange<Solution_>> problemChangeList);

    /**
     * Terminates the solver if running.
     * If the solver has not yet started, the job will be canceled and the solver will never start.
     * Does nothing if the solver already terminated.
     * <p>
     * Waits for the termination or cancellation to complete before returning.
     * During termination, a {@code bestSolutionConsumer} could still be called. When the solver terminates,
     * the {@code finalBestSolutionConsumer} is executed with the latest best solution.
     * These consumers run on a consumer thread independently of the termination and may still run even after
     * this method returns.
     * <p>
     * It waits for no more than one minute, at which point it returns.
     * In this case the solver might still be running in the background, but it will be terminated as soon as possible.
     * Any best solutions from that point on will be ignored.
     */
    void terminateEarly();

    /**
     * @return true if {@link SolverJob#terminateEarly} has been called since the underlying {@link Solver}
     *         started solving.
     */
    boolean isTerminatedEarly();

    /**
     * Waits if necessary for the solver to complete and then returns the final best {@link PlanningSolution}.
     * This is the {@link SolverRunResult#solution() solution} of {@link #getResult()},
     * or the original problem if the job was terminated before solving started.
     *
     * @return never null, but it could be the original uninitialized problem
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     */
    default Solution_ getFinalBestSolution() throws InterruptedException, ExecutionException {
        return getResult().solution();
    };

    /**
     * Waits if necessary for the solver to complete and then returns the final best {@link PlanningSolution}
     * together with the {@link SolverRunSummary summary} of the run.
     * The summary values are totals for the whole run, including every restart after a problem change.
     *
     * @return never null
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     * @throws CancellationException if the job was terminated before solving started,
     *         or if its solver thread was cancelled because early termination took too long
     */
    SolverRunResult<Solution_> getResult() throws InterruptedException, ExecutionException;

    /**
     * Returns the {@link Duration} spent solving since the last start.
     * If it hasn't started it yet, it returns {@link Duration#ZERO}.
     * If it hasn't ended yet, it returns the time between the last start and now.
     * If it has ended already, it returns the time between the last start and the ending.
     * <p>
     * The solver restarts after each {@link ProblemChange problem change},
     * so this value covers only the time since the last problem change.
     * For totals of the whole run, use {@link #getResult()}.
     *
     * @return the {@link Duration} spent solving since the last (re)start, at least 0
     * @see #getResult()
     */
    Duration getSolvingDuration();

    /**
     * Return the number of score calculations since the last start.
     * If it hasn't started yet, it returns 0.
     * If it hasn't ended yet, it returns the number of score calculations so far.
     * If it has ended already, it returns the total number of score calculations that occurred during solving.
     * <p>
     * The solver restarts after each {@link ProblemChange problem change},
     * so this value covers only the time since the last problem change.
     * For totals of the whole run, use {@link #getResult()}.
     *
     * @return the number of score calculations that had occurred during solving since the last (re)start, at least 0
     * @see #getResult()
     */
    long getScoreCalculationCount();

    /**
     * Return the number of move evaluations since the last start.
     * If it hasn't started yet, it returns 0.
     * If it hasn't ended yet, it returns the number of moves evaluations so far.
     * If it has ended already, it returns the total number of move evaluations that occurred during solving.
     * <p>
     * The solver restarts after each {@link ProblemChange problem change},
     * so this value covers only the time since the last problem change.
     * For totals of the whole run, use {@link #getResult()}.
     *
     * @return the number of move evaluations that had occurred during solving since the last (re)start, at least 0
     * @see #getResult()
     */
    long getMoveEvaluationCount();

    /**
     * Return the {@link ProblemSizeStatistics} for the {@link PlanningSolution problem} submitted to the
     * {@link SolverManager}.
     *
     * @see #getResult()
     */
    ProblemSizeStatistics getProblemSizeStatistics();

    /**
     * Return the average number of score calculations per second since the last start.
     * If it hasn't started yet, it returns 0.
     * If it hasn't ended yet, it returns the average number of score calculations per second so far.
     * If it has ended already, it returns the average number of score calculations per second during solving.
     * <p>
     * The solver restarts after each {@link ProblemChange problem change},
     * so this value covers only the time since the last problem change.
     * For totals of the whole run, use {@link #getResult()}.
     *
     * @return the average number of score calculations per second that had occurred during solving
     *         since the last (re)start, at least 0
     * @see #getResult()
     */
    long getScoreCalculationSpeed();

    /**
     * Return the average number of move evaluations per second since the last start.
     * If it hasn't started yet, it returns 0.
     * If it hasn't ended yet, it returns the average number of move evaluations per second so far.
     * If it has ended already, it returns the average number of move evaluations per second during solving.
     * <p>
     * The solver restarts after each {@link ProblemChange problem change},
     * so this value covers only the time since the last problem change.
     * For totals of the whole run, use {@link #getResult()}.
     *
     * @return the average number of move evaluations per second that had occurred during solving
     *         since the last (re)start, at least 0
     * @see #getResult()
     */
    long getMoveEvaluationSpeed();
}
