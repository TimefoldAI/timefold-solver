package ai.timefold.solver.core.api.solver;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

/**
 * Provides a fluent contract that allows customization and submission of planning problems to solve.
 * <p>
 * A {@link SolverManager} can solve multiple planning problems and can be used across different threads.
 * <p>
 * Hence, it is possible to have multiple distinct build configurations that are scheduled to run by the {@link SolverManager}
 * instance.
 * <p>
 * To solve a planning problem, set the problem configuration: {@link #withProblemId(Object)},
 * {@link #withProblemFinder(Function)} and {@link #withProblem(Object)}.
 * <p>
 * Then solve it by calling {@link #run()}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <ProblemId_> the ID type of submitted problem, such as {@link Long} or {@link UUID}.
 */
public interface SolverJobBuilder<Solution_, ProblemId_> {

    /**
     * Sets the problem id.
     *
     * @param problemId a ID for each planning problem. This must be unique.
     * @return this
     */
    @NonNull
    SolverJobBuilder<Solution_, ProblemId_> withProblemId(@NonNull ProblemId_ problemId);

    /**
     * Sets the problem definition.
     *
     * @param problem a {@link PlanningSolution} usually with uninitialized planning variables
     * @return this
     */
    default @NonNull SolverJobBuilder<Solution_, ProblemId_> withProblem(@NonNull Solution_ problem) {
        return withProblemFinder(id -> problem);
    }

    /**
     * Sets the mapping function to the problem definition.
     *
     * @param problemFinder a function that returns a {@link PlanningSolution}, usually with uninitialized planning variables
     * @return this
     */
    @NonNull
    SolverJobBuilder<Solution_, ProblemId_>
            withProblemFinder(@NonNull Function<? super ProblemId_, ? extends Solution_> problemFinder);

    /**
     * Sets the best solution consumer, which may be called multiple times during the solving process.
     * <p>
     * Don't apply any changes to the solution instance while the solver runs.
     * The solver's best solution instance is the same as the one in the event,
     * and any modifications may lead to solver corruption due to its internal reuse.
     *
     * @param bestSolutionConsumer called multiple times for each new best solution on a consumer thread
     * @return this
     */
    @NonNull
    SolverJobBuilder<Solution_, ProblemId_> withBestSolutionConsumer(@NonNull Consumer<? super Solution_> bestSolutionConsumer);

    /**
     * Sets the final best solution consumer, which is called at the end of the solving process and returns the final
     * best solution.
     *
     * @param finalBestSolutionConsumer called only once at the end of the solving process on a consumer thread
     * @return this
     */
    @NonNull
    SolverJobBuilder<Solution_, ProblemId_>
            withFinalBestSolutionConsumer(@NonNull Consumer<? super Solution_> finalBestSolutionConsumer);

    /**
     * As defined by #withFirstInitializedSolutionConsumer(FirstInitializedSolutionConsumer).
     *
     * @deprecated Use {@link #withFirstInitializedSolutionConsumer(FirstInitializedSolutionConsumer)} instead.
     */
    @Deprecated(forRemoval = true, since = "1.19.0")
    @NonNull
    default SolverJobBuilder<Solution_, ProblemId_>
            withFirstInitializedSolutionConsumer(@NonNull Consumer<? super Solution_> firstInitializedSolutionConsumer) {
        return withFirstInitializedSolutionConsumer(
                (solution, isTerminatedEarly) -> firstInitializedSolutionConsumer.accept(solution));
    }

    /**
     * Sets the consumer of the first initialized solution,
     * the beginning of the actual optimization process.
     * First initialized solution is the solution at the end of the last phase
     * that immediately precedes the first local search phase.
     *
     * @param firstInitializedSolutionConsumer called only once before starting the first Local Search phase
     * @return this
     */
    @NonNull
    SolverJobBuilder<Solution_, ProblemId_> withFirstInitializedSolutionConsumer(
            @NonNull FirstInitializedSolutionConsumer<? super Solution_> firstInitializedSolutionConsumer);

    /**
     * Sets the consumer for when the solver starts its solving process.
     *
     * @param solverJobStartedConsumer never null, called only once when the solver is starting the solving process
     * @return this, never null
     */
    SolverJobBuilder<Solution_, ProblemId_> withSolverJobStartedConsumer(Consumer<? super Solution_> solverJobStartedConsumer);

    /**
     * Sets the custom exception handler.
     *
     * @param exceptionHandler called if an exception or error occurs. If null it defaults to logging the
     *        exception as an error.
     * @return this
     */
    @NonNull
    SolverJobBuilder<Solution_, ProblemId_>
            withExceptionHandler(@NonNull BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler);

    /**
     * Sets the solver config override.
     *
     * @param solverConfigOverride allows overriding the default behavior of {@link Solver}
     * @return this
     */
    @NonNull
    SolverJobBuilder<Solution_, ProblemId_> withConfigOverride(@NonNull SolverConfigOverride<Solution_> solverConfigOverride);

    /**
     * Submits a planning problem to solve and returns immediately. The planning problem is solved on a solver {@link Thread},
     * as soon as one is available.
     */
    @NonNull
    SolverJob<Solution_, ProblemId_> run();

    /**
     * A consumer that accepts the first initialized solution.
     *
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     */
    @NullMarked
    interface FirstInitializedSolutionConsumer<Solution_> {

        /**
         * Accepts the first solution after initialization.
         *
         * @param solution the first solution after initialization phase(s) finished
         * @param isTerminatedEarly false in most common cases.
         *        True if the solver was terminated early, before the solution could be fully initialized,
         *        typically as a result of construction heuristic running for too long
         *        and tripping a time-based termination condition.
         *        In that case, there will likely be no other phase after this one
         *        and the solver will terminate as well, without launching any optimizing phase.
         *        Therefore, the solution captured with {@link SolverJobBuilder#withBestSolutionConsumer(Consumer)}
         *        will likely be unchanged from this one.
         */
        void accept(Solution_ solution, boolean isTerminatedEarly);

    }

}
