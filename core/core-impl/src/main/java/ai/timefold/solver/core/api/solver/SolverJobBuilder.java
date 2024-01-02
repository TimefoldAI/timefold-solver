package ai.timefold.solver.core.api.solver;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

/**
 * Provides a fluid contract that allows customization and submission of planning problems to solve.
 * <p>
 * To create a {@link SolverJob}, use {@link #run()}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <ProblemId_> the ID type of submitted problem, such as {@link Long} or {@link UUID}.
 */
public interface SolverJobBuilder<Solution_, ProblemId_> {

    // ************************************************************************
    // With methods
    // ************************************************************************
    SolverJobBuilder<Solution_, ProblemId_> withProblemId(ProblemId_ id);

    default SolverJobBuilder<Solution_, ProblemId_> withProblem(Solution_ problem) {
        return withProblemFinder(id -> problem);
    }

    SolverJobBuilder<Solution_, ProblemId_> withProblemFinder(Function<? super ProblemId_, ? extends Solution_> problemFinder);

    SolverJobBuilder<Solution_, ProblemId_> withBestSolutionConsumer(Consumer<? super Solution_> bestSolutionConsumer);

    SolverJobBuilder<Solution_, ProblemId_>
            withFinalBestSolutionConsumer(Consumer<? super Solution_> finalBestSolutionConsumer);

    SolverJobBuilder<Solution_, ProblemId_>
            withExceptionHandler(BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler);

    // ************************************************************************
    // Overriding settings
    // ************************************************************************
    SolverJobBuilder<Solution_, ProblemId_> withTerminationConfig(TerminationConfig terminationConfig);

    /**
     * Submits a planning problem to solve and returns immediately. The planning problem is solved on a solver {@link Thread},
     * as soon as one is available.
     *
     * @return never null
     */
    SolverJob<Solution_, ProblemId_> run();
}
