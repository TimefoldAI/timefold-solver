package ai.timefold.solver.core.api.solver;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

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
     * @param problemId never null, a ID for each planning problem. This must be unique.
     * @return this, never null
     */
    SolverJobBuilder<Solution_, ProblemId_> withProblemId(ProblemId_ problemId);

    /**
     * Sets the problem definition.
     *
     * @param problem never null, a {@link PlanningSolution} usually with uninitialized planning variables
     * @return this, never null
     */
    default SolverJobBuilder<Solution_, ProblemId_> withProblem(Solution_ problem) {
        return withProblemFinder(id -> problem);
    }

    /**
     * Sets the mapping function to the problem definition.
     *
     * @param problemFinder never null, a function that returns a {@link PlanningSolution}, usually with uninitialized
     *        planning variables
     * @return this, never null
     */
    SolverJobBuilder<Solution_, ProblemId_> withProblemFinder(Function<? super ProblemId_, ? extends Solution_> problemFinder);

    /**
     * Sets the best solution consumer, which may be called multiple times during the solving process.
     *
     * @param bestSolutionConsumer never null, called multiple times for each new best solution on a consumer thread
     * @return this, never null
     */
    SolverJobBuilder<Solution_, ProblemId_> withBestSolutionConsumer(Consumer<? super Solution_> bestSolutionConsumer);

    /**
     * Sets the final best solution consumer, which is called at the end of the solving process and returns the final
     * best solution.
     *
     * @param finalBestSolutionConsumer never null, called only once at the end of the solving process on a consumer thread
     * @return this, never null
     */
    SolverJobBuilder<Solution_, ProblemId_>
            withFinalBestSolutionConsumer(Consumer<? super Solution_> finalBestSolutionConsumer);

    /**
     * Sets the consumer of the first initialized solution. First initialized solution is the solution at the end of
     * the last phase that immediately precedes the first local search phase. This solution marks the beginning of actual
     * optimization process.
     *
     * @param firstInitializedSolutionConsumer never null, called only once before starting the first Local Search phase
     * @return this, never null
     */
    SolverJobBuilder<Solution_, ProblemId_>
            withFirstInitializedSolutionConsumer(Consumer<? super Solution_> firstInitializedSolutionConsumer);

    /**
     * Sets the custom exception handler.
     *
     * @param exceptionHandler never null, called if an exception or error occurs. If null it defaults to logging the
     *        exception as an error.
     * @return this, never null
     */
    SolverJobBuilder<Solution_, ProblemId_>
            withExceptionHandler(BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler);

    /**
     * Sets the solver config override.
     *
     * @param solverConfigOverride never null, allows overriding the default behavior of {@link Solver}
     * @return this, never null
     */
    SolverJobBuilder<Solution_, ProblemId_> withConfigOverride(SolverConfigOverride<Solution_> solverConfigOverride);

    /**
     * Submits a planning problem to solve and returns immediately. The planning problem is solved on a solver {@link Thread},
     * as soon as one is available.
     *
     * @return never null
     */
    SolverJob<Solution_, ProblemId_> run();
}
