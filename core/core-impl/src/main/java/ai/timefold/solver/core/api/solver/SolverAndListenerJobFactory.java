package ai.timefold.solver.core.api.solver;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

/**
 * Provides a fluid contract that allows customization and submission of planning problems to solve. It differs {@link SolverJobFactory}
 * because it allows to listen to intermediate best solutions.
 * <p>
 * To create a {@link SolverJob}, use {@link #solveAndListen()}.
 *
 * @param <Solution_>  the solution type, the class with the {@link PlanningSolution} annotation
 * @param <ProblemId_> the ID type of a submitted problem, such as {@link Long} or {@link UUID}.
 */
public interface SolverAndListenerJobFactory<Solution_, ProblemId_> extends SolverJobFactory<Solution_, ProblemId_> {

    // ************************************************************************
    // With methods
    // ************************************************************************
    @Override
    SolverAndListenerJobFactory<Solution_, ProblemId_> withFinalBestSolutionConsumer(Consumer<? super Solution_> finalBestSolutionConsumer);

    @Override
    SolverAndListenerJobFactory<Solution_, ProblemId_> withExceptionHandler(BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler);

    @Override
    SolverAndListenerJobFactory<Solution_, ProblemId_> withTerminationConfig(TerminationConfig terminationConfig);

    /**
     * Submits a planning problem to solve and returns immediately.
     * The planning problem is solved on a solver {@link Thread}, as soon as one is available.
     * To retrieve the final best solution, use {@link SolverJob#getFinalBestSolution()}.
     *
     * @return never null
     */
    SolverJob<Solution_, ProblemId_> solveAndListen();
}
