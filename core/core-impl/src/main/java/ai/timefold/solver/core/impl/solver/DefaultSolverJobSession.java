package ai.timefold.solver.core.impl.solver;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverJobBuilder;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.SolverJobConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

/**
 * A {@link SolverManager} can solve multiple planning problems and can be used across different threads. The session of
 * solver manager is responsible for constructing and solving a specific configuration problem. Hence, it is possible to
 * have multiple distinct sessions that are scheduled to run by the SolverManager implementation.
 * <p>
 * To solve a planning problem, set the problem configuration: {@link #withProblemId(Object)},
 * {@link #withProblemFinder(Function)} and {@link #withProblem(Object)}. Then solve it by calling {@link #run()}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <ProblemId_> the ID type of submitted problem, such as {@link Long} or {@link UUID}.
 *
 * @see DefaultSolverManager
 */
public class DefaultSolverJobSession<Solution_, ProblemId_> implements SolverJobBuilder<Solution_, ProblemId_> {

    private final SolverManager<Solution_, ProblemId_> solverManager;
    private final SolverJobConfig<Solution_, ProblemId_> solverJobConfig;

    public DefaultSolverJobSession(SolverManager<Solution_, ProblemId_> solverManager) {
        this.solverManager = Objects.requireNonNull(solverManager, "The SolverManager (" + solverManager + ") cannot be null.");
        this.solverJobConfig = new SolverJobConfig<>();
    }

    @Override
    public SolverJobBuilder<Solution_, ProblemId_> withProblemId(ProblemId_ problemId) {
        this.solverJobConfig.withProblemId(problemId);
        return this;
    }

    @Override
    public SolverJobBuilder<Solution_, ProblemId_> withProblemFinder(Function<? super ProblemId_, ? extends Solution_> problemFinder) {
        this.solverJobConfig.withProblemFinder(problemFinder);
        return this;
    }

    @Override
    public SolverJobBuilder<Solution_, ProblemId_> withBestSolutionConsumer(Consumer<? super Solution_> bestSolutionConsumer) {
        this.solverJobConfig.withBestSolutionConsumer(bestSolutionConsumer);
        return this;
    }

    @Override
    public SolverJobBuilder<Solution_, ProblemId_> withFinalBestSolutionConsumer(Consumer<? super Solution_> finalBestSolutionConsumer) {
        this.solverJobConfig.withFinalBestSolutionConsumer(finalBestSolutionConsumer);
        return this;
    }

    @Override
    public SolverJobBuilder<Solution_, ProblemId_> withExceptionHandler(BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler) {
        this.solverJobConfig.withExceptionHandler(exceptionHandler);
        return this;
    }

    @Override
    public SolverJobBuilder<Solution_, ProblemId_> withTerminationConfig(TerminationConfig terminationConfig) {
        this.solverJobConfig.withTerminationConfig(terminationConfig);
        return this;
    }

    @Override
    public SolverJob<Solution_, ProblemId_> run() {
        return solverManager.solve(solverJobConfig);
    }
}
