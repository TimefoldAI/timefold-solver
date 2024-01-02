package ai.timefold.solver.core.config.solver;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

/**
 * Settings used by {@link ai.timefold.solver.core.api.solver.SolverManager} to submit and solve problems. It also
 * includes settings to override default {@link ai.timefold.solver.core.api.solver.Solver} configuration.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <ProblemId_> the ID type of a submitted problem, such as {@link Long} or {@link UUID}.
 */
public class SolverJobConfig<Solution_, ProblemId_> {

    private ProblemId_ problemId;
    private Function<? super ProblemId_, ? extends Solution_> problemFinder;
    private Consumer<? super Solution_> bestSolutionConsumer;
    private Consumer<? super Solution_> finalBestSolutionConsumer;
    private BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler;
    private TerminationConfig terminationConfig;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public ProblemId_ getProblemId() {
        return problemId;
    }

    public Function<? super ProblemId_, ? extends Solution_> getProblemFinder() {
        return problemFinder;
    }

    public Consumer<? super Solution_> getBestSolutionConsumer() {
        return bestSolutionConsumer;
    }

    public Consumer<? super Solution_> getFinalBestSolutionConsumer() {
        return finalBestSolutionConsumer;
    }

    public BiConsumer<? super ProblemId_, ? super Throwable> getExceptionHandler() {
        return exceptionHandler;
    }

    public TerminationConfig getTerminationConfig() {
        return terminationConfig;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************
    public SolverJobConfig<Solution_, ProblemId_> withProblemId(ProblemId_ problemId) {
        this.problemId = problemId;
        return this;
    }

    public SolverJobConfig<Solution_, ProblemId_> withProblem(Solution_ problem) {
        return withProblemFinder(id -> problem);
    }

    public SolverJobConfig<Solution_, ProblemId_>
            withProblemFinder(Function<? super ProblemId_, ? extends Solution_> problemFinder) {
        this.problemFinder = problemFinder;
        return this;
    }

    /**
     * It is never null for {@link ai.timefold.solver.core.api.solver.SolutionManager}, called multiple times, on a consumer
     * thread
     */
    public SolverJobConfig<Solution_, ProblemId_> withBestSolutionConsumer(Consumer<? super Solution_> bestSolutionConsumer) {
        this.bestSolutionConsumer = bestSolutionConsumer;
        return this;
    }

    /**
     * May be null, called only once, at the end, on a consumer thread.
     */
    public SolverJobConfig<Solution_, ProblemId_>
            withFinalBestSolutionConsumer(Consumer<? super Solution_> finalBestSolutionConsumer) {
        this.finalBestSolutionConsumer = finalBestSolutionConsumer;
        return this;
    }

    public SolverJobConfig<Solution_, ProblemId_>
            withExceptionHandler(BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    public SolverJobConfig<Solution_, ProblemId_> withTerminationConfig(TerminationConfig terminationConfig) {
        this.terminationConfig = terminationConfig;
        return this;
    }
}
