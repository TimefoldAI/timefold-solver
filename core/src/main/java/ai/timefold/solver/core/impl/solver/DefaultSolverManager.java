package ai.timefold.solver.core.impl.solver;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.solver.SolverConfigOverride;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverJobBuilder;
import ai.timefold.solver.core.api.solver.SolverJobBuilder.FirstInitializedSolutionConsumer;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <ProblemId_> the ID type of submitted problem, such as {@link Long} or {@link UUID}.
 */
public final class DefaultSolverManager<Solution_, ProblemId_> implements SolverManager<Solution_, ProblemId_> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSolverManager.class);

    private final BiConsumer<ProblemId_, Throwable> defaultExceptionHandler;
    private final SolverFactory<Solution_> solverFactory;
    private final ExecutorService solverThreadPool;
    private final ConcurrentMap<Object, DefaultSolverJob<Solution_, ProblemId_>> problemIdToSolverJobMap;

    public DefaultSolverManager(SolverFactory<Solution_> solverFactory, SolverManagerConfig solverManagerConfig) {
        this.defaultExceptionHandler =
                (problemId, throwable) -> LOGGER.error("Solving failed for problemId ({}).", problemId, throwable);
        this.solverFactory = solverFactory;
        validateSolverFactory();
        var parallelSolverCount = solverManagerConfig.resolveParallelSolverCount();
        var threadFactoryClass = solverManagerConfig.getThreadFactoryClass();
        var threadFactory = threadFactoryClass == null ? Executors.defaultThreadFactory()
                : ConfigUtils.newInstance(solverManagerConfig, "threadFactoryClass", threadFactoryClass);
        solverThreadPool = Executors.newFixedThreadPool(parallelSolverCount, threadFactory);
        problemIdToSolverJobMap = new ConcurrentHashMap<>(parallelSolverCount * 10);
    }

    public SolverFactory<Solution_> getSolverFactory() {
        return solverFactory;
    }

    private void validateSolverFactory() {
        solverFactory.buildSolver();
    }

    private ProblemId_ getProblemIdOrThrow(ProblemId_ problemId) {
        return Objects.requireNonNull(problemId, "Invalid problemId (null) given to SolverManager.");
    }

    private DefaultSolverJob<Solution_, ProblemId_> getSolverJob(ProblemId_ problemId) {
        return problemIdToSolverJobMap.get(getProblemIdOrThrow(problemId));
    }

    @Override
    public @NonNull SolverJobBuilder<Solution_, ProblemId_> solveBuilder() {
        return new DefaultSolverJobBuilder<>(this);
    }

    SolverJob<Solution_, ProblemId_> solveAndListen(ProblemId_ problemId,
            Function<? super ProblemId_, ? extends Solution_> problemFinder,
            Consumer<? super Solution_> bestSolutionConsumer,
            Consumer<? super Solution_> finalBestSolutionConsumer,
            FirstInitializedSolutionConsumer<? super Solution_> initializedSolutionConsumer,
            Consumer<? super Solution_> solverJobStartedConsumer,
            BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler,
            SolverConfigOverride<Solution_> solverConfigOverride) {
        if (bestSolutionConsumer == null) {
            throw new IllegalStateException("The consumer bestSolutionConsumer is required.");
        }
        return solve(getProblemIdOrThrow(problemId), problemFinder, bestSolutionConsumer, finalBestSolutionConsumer,
                initializedSolutionConsumer, solverJobStartedConsumer, exceptionHandler, solverConfigOverride);
    }

    SolverJob<Solution_, ProblemId_> solve(ProblemId_ problemId,
            Function<? super ProblemId_, ? extends Solution_> problemFinder,
            Consumer<? super Solution_> bestSolutionConsumer,
            Consumer<? super Solution_> finalBestSolutionConsumer,
            FirstInitializedSolutionConsumer<? super Solution_> initializedSolutionConsumer,
            Consumer<? super Solution_> solverJobStartedConsumer,
            BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler,
            SolverConfigOverride<Solution_> configOverride) {
        var solver = solverFactory.buildSolver(configOverride);
        ((DefaultSolver<Solution_>) solver).setMonitorTagMap(Map.of("problem.id", problemId.toString()));
        BiConsumer<? super ProblemId_, ? super Throwable> finalExceptionHandler = (exceptionHandler != null)
                ? exceptionHandler
                : defaultExceptionHandler;
        var solverJob = problemIdToSolverJobMap
                .compute(problemId, (key, oldSolverJob) -> {
                    if (oldSolverJob != null) {
                        // TODO Future features: automatically restart solving by calling reloadProblem()
                        throw new IllegalStateException("The problemId (" + problemId + ") is already solving.");
                    } else {
                        return new DefaultSolverJob<>(this, solver, problemId, problemFinder, bestSolutionConsumer,
                                finalBestSolutionConsumer, initializedSolutionConsumer, solverJobStartedConsumer,
                                finalExceptionHandler);
                    }
                });
        var future = solverThreadPool.submit(solverJob);
        solverJob.setFinalBestSolutionFuture(future);
        return solverJob;
    }

    @Override
    public @NonNull SolverStatus getSolverStatus(@NonNull ProblemId_ problemId) {
        var solverJob = getSolverJob(problemId);
        if (solverJob == null) {
            return SolverStatus.NOT_SOLVING;
        }
        return solverJob.getSolverStatus();
    }

    @Override
    public @NonNull CompletableFuture<Void> addProblemChanges(@NonNull ProblemId_ problemId,
            @NonNull List<ProblemChange<Solution_>> problemChangeList) {
        var solverJob = getSolverJob(problemId);
        if (solverJob == null) {
            // We cannot distinguish between "already terminated" and "never solved" without causing a memory leak.
            throw new IllegalStateException(
                    "Cannot add the problem changes (%s) because there is no solver solving the problemId (%s)."
                            .formatted(problemChangeList, problemId));
        }
        return solverJob.addProblemChanges(problemChangeList);
    }

    @Override
    public void terminateEarly(@NonNull ProblemId_ problemId) {
        var solverJob = getSolverJob(problemId);
        if (solverJob == null) {
            // We cannot distinguish between "already terminated" and "never solved" without causing a memory leak.
            LOGGER.debug("Ignoring terminateEarly() call because problemId ({}) is not solving.", problemId);
            return;
        }
        solverJob.terminateEarly();
    }

    @Override
    public void close() {
        solverThreadPool.shutdownNow();
        problemIdToSolverJobMap.values().forEach(DefaultSolverJob::close);
    }

    void unregisterSolverJob(ProblemId_ problemId) {
        problemIdToSolverJobMap.remove(getProblemIdOrThrow(problemId));
    }

}
