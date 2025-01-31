package ai.timefold.solver.core.api.solver;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.solver.change.ProblemChange;

import org.jspecify.annotations.NonNull;

/**
 * Represents a {@link PlanningSolution problem} that has been submitted to solve on the {@link SolverManager}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <ProblemId_> the ID type of a submitted problem, such as {@link Long} or {@link UUID}.
 */
public interface SolverJob<Solution_, ProblemId_> {

    /**
     * @return a value given to {@link SolverManager#solve(Object, Object, Consumer)}
     *         or {@link SolverManager#solveAndListen(Object, Object, Consumer)}
     */
    @NonNull
    ProblemId_ getProblemId();

    /**
     * Returns whether the {@link Solver} is scheduled to solve, actively solving or not.
     * <p>
     * Returns {@link SolverStatus#NOT_SOLVING} if the solver already terminated.
     *
     */
    @NonNull
    SolverStatus getSolverStatus();

    /**
     * As defined by {@link #addProblemChanges(List)}, only for a single problem change.
     * Prefer to submit multiple {@link ProblemChange}s at once to reduce the considerable overhead of multiple calls.
     */
    @NonNull
    default CompletableFuture<Void> addProblemChange(@NonNull ProblemChange<Solution_> problemChange) {
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
    @NonNull
    CompletableFuture<Void> addProblemChanges(@NonNull List<ProblemChange<Solution_>> problemChangeList);

    /**
     * Terminates the solver or cancels the solver job if it hasn't (re)started yet.
     * <p>
     * Does nothing if the solver already terminated.
     * <p>
     * Waits for the termination or cancellation to complete before returning.
     * During termination, a {@code bestSolutionConsumer} could still be called. When the solver terminates,
     * the {@code finalBestSolutionConsumer} is executed with the latest best solution.
     * These consumers run on a consumer thread independently of the termination and may still run even after
     * this method returns.
     */
    void terminateEarly();

    /**
     * @return true if {@link SolverJob#terminateEarly} has been called since the underlying {@link Solver}
     *         started solving.
     */
    boolean isTerminatedEarly();

    /**
     * Waits if necessary for the solver to complete and then returns the final best {@link PlanningSolution}.
     *
     * @return never null, but it could be the original uninitialized problem
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     */
    @NonNull
    Solution_ getFinalBestSolution() throws InterruptedException, ExecutionException;

    /**
     * Returns the {@link Duration} spent solving since the last start.
     * If it hasn't started it yet, it returns {@link Duration#ZERO}.
     * If it hasn't ended yet, it returns the time between the last start and now.
     * If it has ended already, it returns the time between the last start and the ending.
     *
     * @return the {@link Duration} spent solving since the last (re)start, at least 0
     */
    @NonNull
    Duration getSolvingDuration();

    /**
     * Return the number of score calculations since the last start.
     * If it hasn't started yet, it returns 0.
     * If it hasn't ended yet, it returns the number of score calculations so far.
     * If it has ended already, it returns the total number of score calculations that occurred during solving.
     *
     * @return the number of score calculations that had occurred during solving since the last (re)start, at least 0
     */
    long getScoreCalculationCount();

    /**
     * Return the number of move evaluations since the last start.
     * If it hasn't started yet, it returns 0.
     * If it hasn't ended yet, it returns the number of moves evaluations so far.
     * If it has ended already, it returns the total number of move evaluations that occurred during solving.
     *
     * @return the number of move evaluations that had occurred during solving since the last (re)start, at least 0
     */
    long getMoveEvaluationCount();

    /**
     * Return the {@link ProblemSizeStatistics} for the {@link PlanningSolution problem} submitted to the
     * {@link SolverManager}.
     *
     */
    @NonNull
    ProblemSizeStatistics getProblemSizeStatistics();

    /**
     * Return the average number of score calculations per second since the last start.
     * If it hasn't started yet, it returns 0.
     * If it hasn't ended yet, it returns the average number of score calculations per second so far.
     * If it has ended already, it returns the average number of score calculations per second during solving.
     *
     * @return the average number of score calculations per second that had occurred during solving
     *         since the last (re)start, at least 0
     */
    long getScoreCalculationSpeed();

    /**
     * Return the average number of move evaluations per second since the last start.
     * If it hasn't started yet, it returns 0.
     * If it hasn't ended yet, it returns the average number of move evaluations per second so far.
     * If it has ended already, it returns the average number of move evaluations per second during solving.
     *
     * @return the average number of move evaluations per second that had occurred during solving
     *         since the last (re)start, at least 0
     */
    long getMoveEvaluationSpeed();
}
