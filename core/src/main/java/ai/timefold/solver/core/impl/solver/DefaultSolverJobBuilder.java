package ai.timefold.solver.core.impl.solver;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.solver.SolverConfigOverride;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverJobBuilder;
import ai.timefold.solver.core.api.solver.event.FinalBestSolutionEvent;
import ai.timefold.solver.core.api.solver.event.FirstInitializedSolutionEvent;
import ai.timefold.solver.core.api.solver.event.NewBestSolutionEvent;
import ai.timefold.solver.core.api.solver.event.SolverJobStartedEvent;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public final class DefaultSolverJobBuilder<Solution_> implements SolverJobBuilder<Solution_> {

    private final DefaultSolverManager<Solution_> solverManager;
    private @Nullable Object problemId;
    private @Nullable Function<? super Object, ? extends Solution_> problemFinder;
    private @Nullable Consumer<NewBestSolutionEvent<Solution_>> bestSolutionConsumer;
    private @Nullable Consumer<FinalBestSolutionEvent<Solution_>> finalBestSolutionConsumer;
    private @Nullable Consumer<FirstInitializedSolutionEvent<Solution_>> initializedSolutionConsumer;
    private @Nullable Consumer<SolverJobStartedEvent<Solution_>> solverJobStartedConsumer;
    private @Nullable BiConsumer<? super Object, ? super Throwable> exceptionHandler;
    private @Nullable SolverConfigOverride<Solution_> solverConfigOverride;

    public DefaultSolverJobBuilder(DefaultSolverManager<Solution_> solverManager) {
        this.solverManager = Objects.requireNonNull(solverManager, "The SolverManager (" + solverManager + ") cannot be null.");
    }

    @Override
    public SolverJobBuilder<Solution_> withProblemId(Object problemId) {
        this.problemId = Objects.requireNonNull(problemId, "Invalid problemId (null) given to SolverJobBuilder.");
        return this;
    }

    @Override
    public SolverJobBuilder<Solution_>
            withProblemFinder(Function<? super Object, ? extends Solution_> problemFinder) {
        this.problemFinder = Objects.requireNonNull(problemFinder, "Invalid problemFinder (null) given to SolverJobBuilder.");
        return this;
    }

    @Override
    public SolverJobBuilder<Solution_>
            withBestSolutionEventConsumer(Consumer<NewBestSolutionEvent<Solution_>> bestSolutionConsumer) {
        this.bestSolutionConsumer =
                Objects.requireNonNull(bestSolutionConsumer, "Invalid bestSolutionConsumer (null) given to SolverJobBuilder.");
        return this;
    }

    @Override
    public SolverJobBuilder<Solution_>
            withFinalBestSolutionEventConsumer(
                    Consumer<FinalBestSolutionEvent<Solution_>> finalBestSolutionConsumer) {
        this.finalBestSolutionConsumer = Objects.requireNonNull(finalBestSolutionConsumer,
                "Invalid finalBestSolutionConsumer (null) given to SolverJobBuilder.");
        return this;
    }

    @Override
    public SolverJobBuilder<Solution_>
            withFirstInitializedSolutionEventConsumer(
                    Consumer<FirstInitializedSolutionEvent<Solution_>> firstInitializedSolutionConsumer) {
        this.initializedSolutionConsumer = Objects.requireNonNull(firstInitializedSolutionConsumer,
                "Invalid initializedSolutionConsumer (null) given to SolverJobBuilder.");
        return this;
    }

    @Override
    public SolverJobBuilder<Solution_>
            withSolverJobStartedEventConsumer(Consumer<SolverJobStartedEvent<Solution_>> solverJobStartedConsumer) {
        this.solverJobStartedConsumer = Objects.requireNonNull(solverJobStartedConsumer,
                "Invalid startSolverJobHandler (null) given to SolverJobBuilder.");
        return this;
    }

    @Override
    public SolverJobBuilder<Solution_>
            withExceptionHandler(BiConsumer<? super Object, ? super Throwable> exceptionHandler) {
        this.exceptionHandler =
                Objects.requireNonNull(exceptionHandler, "Invalid exceptionHandler (null) given to SolverJobBuilder.");
        return this;
    }

    @Override
    public SolverJobBuilder<Solution_>
            withConfigOverride(SolverConfigOverride<Solution_> solverConfigOverride) {
        this.solverConfigOverride =
                Objects.requireNonNull(solverConfigOverride, "Invalid solverConfigOverride (null) given to SolverJobBuilder.");
        return this;
    }

    @Override
    public SolverJob<Solution_> run() {
        if (problemId == null) {
            throw new IllegalStateException("The problemId is required.");
        }
        if (problemFinder == null) {
            throw new IllegalStateException("The problemFinder function is required.");
        }
        if (solverConfigOverride == null) {
            // The config is required by SolverFactory and it must be initialized
            this.solverConfigOverride = new SolverConfigOverride<>();
        }

        if (this.bestSolutionConsumer == null) {
            return solverManager.solve(problemId, problemFinder, null, finalBestSolutionConsumer,
                    initializedSolutionConsumer, solverJobStartedConsumer, exceptionHandler, solverConfigOverride);
        } else {
            return solverManager.solveAndListen(problemId, problemFinder, bestSolutionConsumer, finalBestSolutionConsumer,
                    initializedSolutionConsumer, solverJobStartedConsumer, exceptionHandler, solverConfigOverride);
        }
    }
}
