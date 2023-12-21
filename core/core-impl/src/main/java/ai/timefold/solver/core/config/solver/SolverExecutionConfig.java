package ai.timefold.solver.core.config.solver;

import java.time.Duration;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

/**
 * Settings used by {@link ai.timefold.solver.core.api.solver.SolverManager}  to submit and solve problems. It also
 * includes settings to override default {@link ai.timefold.solver.core.api.solver.Solver} configuration.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <ProblemId_> the ID type of a submitted problem, such as {@link Long} or {@link UUID}.
 */
public class SolverExecutionConfig<Solution_, ProblemId_> {

    private Consumer<? super Solution_> bestSolutionConsumer;
    private Consumer<? super Solution_> finalBestSolutionConsumer;
    private BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler;
    private TerminationConfig terminationConfig;
    private boolean singleThread = true;
    private boolean multiThread = false;
    private String moveThreadCount;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

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

    public boolean isSingleThread() {
        return singleThread;
    }

    public boolean isMultiThread() {
        return multiThread;
    }

    public String getMoveThreadCount() {
        return moveThreadCount;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    /**
     * It is never null for {@link ai.timefold.solver.core.api.solver.SolutionManager#}, called multiple times, on a consumer thread
     */
    public SolverExecutionConfig<Solution_, ProblemId_> withBestSolutionConsumer(Consumer<? super Solution_> bestSolutionConsumer) {
        this.bestSolutionConsumer = bestSolutionConsumer;
        return this;
    }

    /**
     * May be null, called only once, at the end, on a consumer thread.
     */
    public SolverExecutionConfig<Solution_, ProblemId_> withFinalBestSolutionConsumer(Consumer<? super Solution_> finalBestSolutionConsumer) {
        this.finalBestSolutionConsumer = finalBestSolutionConsumer;
        return this;
    }

    public SolverExecutionConfig<Solution_, ProblemId_> withExceptionHandler(BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    private void ensureTerminationConfig() {
        if (terminationConfig == null) {
            this.terminationConfig = new TerminationConfig();
        }
    }

    public SolverExecutionConfig<Solution_, ProblemId_> withTimeSpentTermination(Duration termination) {
        ensureTerminationConfig();
        this.terminationConfig.withSpentLimit(termination);
        return this;
    }

    public SolverExecutionConfig<Solution_, ProblemId_> withUnimprovedTimeSpentTermination(Duration termination) {
        ensureTerminationConfig();
        this.terminationConfig.withUnimprovedSpentLimit(termination);
        return this;
    }

    public SolverExecutionConfig<Solution_, ProblemId_> withStepCountLimitTermination(int stepCount) {
        ensureTerminationConfig();
        this.terminationConfig.withStepCountLimit(stepCount);
        return this;
    }

    public SolverExecutionConfig<Solution_, ProblemId_> withUnimprovedStepCountLimitTermination(int stepCount) {
        ensureTerminationConfig();
        this.terminationConfig.withUnimprovedStepCountLimit(stepCount);
        return this;
    }

    public SolverExecutionConfig<Solution_, ProblemId_> multiThreaded() {
        this.multiThread = true;
        this.singleThread = false;
        return this;
    }

    public SolverExecutionConfig<Solution_, ProblemId_> multiThreaded(String moveThreadCount) {
        multiThreaded();
        this.moveThreadCount = moveThreadCount;
        return this;
    }

    public SolverExecutionConfig<Solution_, ProblemId_> singleThreaded() {
        this.multiThread = false;
        this.moveThreadCount = null;
        this.singleThread = true;
        return this;
    }
}
