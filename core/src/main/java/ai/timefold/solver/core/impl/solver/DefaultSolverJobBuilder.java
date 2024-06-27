package ai.timefold.solver.core.impl.solver;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.solver.SolverConfigOverride;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverJobBuilder;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <ProblemId_> the ID type of submitted problem, such as {@link Long} or {@link UUID}.
 */
public final class DefaultSolverJobBuilder<Solution_, ProblemId_> implements SolverJobBuilder<Solution_, ProblemId_> {

    private final DefaultSolverManager<Solution_, ProblemId_> solverManager;
    private ProblemId_ problemId;
    private Function<? super ProblemId_, ? extends Solution_> problemFinder;
    private Consumer<? super Solution_> bestSolutionConsumer;
    private Consumer<? super Solution_> finalBestSolutionConsumer;
    private Consumer<? super Solution_> initializedSolutionConsumer;
    private BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler;
    private SolverConfigOverride<Solution_> solverConfigOverride;

    public DefaultSolverJobBuilder(DefaultSolverManager<Solution_, ProblemId_> solverManager) {
        this.solverManager = Objects.requireNonNull(solverManager, "The SolverManager (" + solverManager + ") cannot be null.");
    }

    @Override
    public SolverJobBuilder<Solution_, ProblemId_> withProblemId(ProblemId_ problemId) {
        this.problemId = Objects.requireNonNull(problemId, "Invalid problemId (null) given to SolverJobBuilder.");
        return this;
    }

    @Override
    public SolverJobBuilder<Solution_, ProblemId_>
            withProblemFinder(Function<? super ProblemId_, ? extends Solution_> problemFinder) {
        this.problemFinder = Objects.requireNonNull(problemFinder, "Invalid problemFinder (null) given to SolverJobBuilder.");
        return this;
    }

    @Override
    public SolverJobBuilder<Solution_, ProblemId_> withBestSolutionConsumer(Consumer<? super Solution_> bestSolutionConsumer) {
        this.bestSolutionConsumer =
                Objects.requireNonNull(bestSolutionConsumer, "Invalid bestSolutionConsumer (null) given to SolverJobBuilder.");
        return this;
    }

    @Override
    public SolverJobBuilder<Solution_, ProblemId_>
            withFinalBestSolutionConsumer(Consumer<? super Solution_> finalBestSolutionConsumer) {
        this.finalBestSolutionConsumer = Objects.requireNonNull(finalBestSolutionConsumer,
                "Invalid finalBestSolutionConsumer (null) given to SolverJobBuilder.");
        return this;
    }

    @Override
    public SolverJobBuilder<Solution_, ProblemId_>
            withFirstInitializedSolutionConsumer(Consumer<? super Solution_> firstInitializedSolutionConsumer) {
        this.initializedSolutionConsumer = Objects.requireNonNull(firstInitializedSolutionConsumer,
                "Invalid initializedSolutionConsumer (null) given to SolverJobBuilder.");
        return this;
    }

    @Override
    public SolverJobBuilder<Solution_, ProblemId_>
            withExceptionHandler(BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler) {
        this.exceptionHandler =
                Objects.requireNonNull(exceptionHandler, "Invalid exceptionHandler (null) given to SolverJobBuilder.");
        return this;
    }

    @Override
    public SolverJobBuilder<Solution_, ProblemId_> withConfigOverride(SolverConfigOverride<Solution_> solverConfigOverride) {
        this.solverConfigOverride =
                Objects.requireNonNull(solverConfigOverride, "Invalid solverConfigOverride (null) given to SolverJobBuilder.");
        return this;
    }

    @Override
    public SolverJob<Solution_, ProblemId_> run() {
        if (solverConfigOverride == null) {
            // The config is required by SolverFactory and it must be initialized
            this.solverConfigOverride = new SolverConfigOverride<>();
        }

        if (this.bestSolutionConsumer == null) {
            return solverManager.solve(problemId, problemFinder, null, finalBestSolutionConsumer,
                    initializedSolutionConsumer, exceptionHandler, solverConfigOverride);
        } else {
            return solverManager.solveAndListen(problemId, problemFinder, bestSolutionConsumer, finalBestSolutionConsumer,
                    initializedSolutionConsumer, exceptionHandler, solverConfigOverride);
        }
    }
}
