package ai.timefold.solver.core.impl.solver;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.solver.ProblemSizeStatistics;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverStatus;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.api.solver.event.BestSolutionChangedEvent;
import ai.timefold.solver.core.api.solver.event.FinalBestSolutionEvent;
import ai.timefold.solver.core.api.solver.event.FirstInitializedSolutionEvent;
import ai.timefold.solver.core.api.solver.event.NewBestSolutionEvent;
import ai.timefold.solver.core.api.solver.event.SolverJobStartedEvent;
import ai.timefold.solver.core.impl.phase.AbstractPhase;
import ai.timefold.solver.core.impl.phase.PossiblyInitializingPhase;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.SolverTermination;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <ProblemId_> the ID type of submitted problem, such as {@link Long} or {@link UUID}.
 */
public final class DefaultSolverJob<Solution_, ProblemId_> implements SolverJob<Solution_, ProblemId_>, Callable<Solution_> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSolverJob.class);
    private static final Duration EARLY_TERMINATION_TIMEOUT = Duration.ofMinutes(1);

    private final DefaultSolverManager<Solution_, ProblemId_> solverManager;
    private final DefaultSolver<Solution_> solver;
    private final ProblemId_ problemId;
    private final Function<? super ProblemId_, ? extends Solution_> problemFinder;
    private final Consumer<NewBestSolutionEvent<Solution_>> bestSolutionConsumer;
    private final Consumer<FinalBestSolutionEvent<Solution_>> finalBestSolutionConsumer;
    private final Consumer<FirstInitializedSolutionEvent<Solution_>> firstInitializedSolutionConsumer;
    private final Consumer<SolverJobStartedEvent<Solution_>> solverJobStartedConsumer;
    private final BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler;

    private final CountDownLatch terminatedLatch;
    private final ReentrantLock solverStatusModifyingLock;
    private final AtomicBoolean terminatedEarly = new AtomicBoolean(false);
    private final BestSolutionHolder<Solution_> bestSolutionHolder = new BestSolutionHolder<>();
    private final AtomicReference<SolverStatus> solverStatus = new AtomicReference<>(SolverStatus.SOLVING_SCHEDULED);
    private final AtomicReference<@Nullable Future<Solution_>> finalBestSolutionFuture = new AtomicReference<>();
    private final AtomicReference<@Nullable ConsumerSupport<Solution_, ProblemId_>> consumerSupport = new AtomicReference<>();
    private final AtomicReference<ProblemSizeStatistics> temporaryProblemSizeStatistics = new AtomicReference<>();

    public DefaultSolverJob(
            DefaultSolverManager<Solution_, ProblemId_> solverManager,
            Solver<Solution_> solver, ProblemId_ problemId,
            Function<? super ProblemId_, ? extends Solution_> problemFinder,
            Consumer<NewBestSolutionEvent<Solution_>> bestSolutionConsumer,
            Consumer<FinalBestSolutionEvent<Solution_>> finalBestSolutionConsumer,
            Consumer<FirstInitializedSolutionEvent<Solution_>> firstInitializedSolutionConsumer,
            Consumer<SolverJobStartedEvent<Solution_>> solverJobStartedConsumer,
            BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler) {
        this.solverManager = solverManager;
        this.problemId = problemId;
        if (!(solver instanceof DefaultSolver)) {
            throw new IllegalStateException("Impossible state: solver is not instance of %s."
                    .formatted(DefaultSolver.class.getSimpleName()));
        }
        this.solver = (DefaultSolver<Solution_>) solver;
        this.problemFinder = problemFinder;
        this.bestSolutionConsumer = bestSolutionConsumer;
        this.finalBestSolutionConsumer = finalBestSolutionConsumer;
        this.firstInitializedSolutionConsumer = firstInitializedSolutionConsumer;
        this.solverJobStartedConsumer = solverJobStartedConsumer;
        this.exceptionHandler = exceptionHandler;
        this.terminatedLatch = new CountDownLatch(1);
        this.solverStatusModifyingLock = new ReentrantLock();
    }

    public void setFinalBestSolutionFuture(Future<Solution_> finalBestSolutionFuture) {
        var oldFuture = this.finalBestSolutionFuture.getAndSet(finalBestSolutionFuture);
        if (oldFuture != null) { // We set this, and we should only set it once.
            throw new IllegalStateException("Impossible state: the finalBestSolutionFuture was already set to (%s)."
                    .formatted(oldFuture));
        }
    }

    @Override
    public @NonNull ProblemId_ getProblemId() {
        return problemId;
    }

    @Override
    public @NonNull SolverStatus getSolverStatus() {
        return solverStatus.get();
    }

    @Override
    public Solution_ call() {
        solverStatusModifyingLock.lock();
        if (solverStatus.get() != SolverStatus.SOLVING_SCHEDULED) {
            // This job has been canceled before it started,
            // or it is already solving
            solverStatusModifyingLock.unlock();
            return problemFinder.apply(problemId);
        }
        try {
            solverStatus.set(SolverStatus.SOLVING_ACTIVE);
            // Create the consumer thread pool only when this solver job is active.
            var currentConsumerSupport = new ConsumerSupport<>(problemId, bestSolutionConsumer, finalBestSolutionConsumer,
                    firstInitializedSolutionConsumer, solverJobStartedConsumer, exceptionHandler, bestSolutionHolder);
            var oldConsumerSupport = this.consumerSupport.getAndSet(currentConsumerSupport);
            if (oldConsumerSupport != null) { // We set this, and we should only set it once.
                throw new IllegalStateException("Impossible state: the consumerSupport was already set to (%s)."
                        .formatted(oldConsumerSupport));
            }
            Solution_ problem = problemFinder.apply(problemId);
            // add a phase lifecycle listener that unlock the solver status lock when solving started
            solver.addPhaseLifecycleListener(new UnlockLockPhaseLifecycleListener());
            // add a phase lifecycle listener that consumes the first initialized solution
            solver.addPhaseLifecycleListener(new FirstInitializedSolutionPhaseLifecycleListener(currentConsumerSupport));
            // add a phase lifecycle listener once when the solver starts its execution
            solver.addPhaseLifecycleListener(new StartSolverJobPhaseLifecycleListener(currentConsumerSupport));
            solver.addEventListener(this::onBestSolutionChangedEvent);
            final Solution_ finalBestSolution = solver.solve(problem);
            currentConsumerSupport.consumeFinalBestSolution(finalBestSolution);
            return finalBestSolution;
        } catch (Throwable e) {
            exceptionHandler.accept(problemId, e);
            bestSolutionHolder.cancelPendingChanges();
            throw new IllegalStateException("Solving failed for problemId (%s)."
                    .formatted(problemId), e);
        } finally {
            if (solverStatusModifyingLock.isHeldByCurrentThread()) {
                // release the lock if we have it (due to solver raising an exception before solving starts);
                // This does not make it possible to do a double terminate in terminateEarly because:
                // 1. The case SOLVING_SCHEDULED is impossible (only set to SOLVING_SCHEDULED in constructor,
                //    and it was set it to SolverStatus.SOLVING_ACTIVE in the method)
                // 2. The case SOLVING_ACTIVE only calls solver.terminateEarly, so it effectively does nothing
                // 3. The case NOT_SOLVING does nothing
                solverStatusModifyingLock.unlock();
            }
            solvingTerminated();
        }
    }

    private void onBestSolutionChangedEvent(BestSolutionChangedEvent<Solution_> bestSolutionChangedEvent) {
        var currentConsumerSupport = consumerSupport.get();
        if (currentConsumerSupport == null) { // We set this, we should only set it once and before any event is emitted.
            LOGGER.warn("""
                    Asked to consume a best solution changed event for problemId (%s), but consumer is not available.
                    The solution is lost. This is likely a bug.
                    Please report this issue to Timefold with details on how to reproduce it.
                    """
                    .formatted(problemId));
            return;
        }
        currentConsumerSupport.consumeIntermediateBestSolution(bestSolutionChangedEvent.getNewBestSolution(),
                bestSolutionChangedEvent.getProducerId(), bestSolutionChangedEvent::isEveryProblemChangeProcessed);
    }

    private void solvingTerminated() {
        solverStatus.set(SolverStatus.NOT_SOLVING);
        solverManager.unregisterSolverJob(problemId);
        terminatedLatch.countDown();
        close();
    }

    @Override
    public @NonNull CompletableFuture<Void> addProblemChanges(@NonNull List<ProblemChange<Solution_>> problemChangeList) {
        Objects.requireNonNull(problemChangeList, () -> "A problem change list for problem (%s) must not be null."
                .formatted(problemId));
        if (problemChangeList.isEmpty()) {
            throw new IllegalArgumentException("The problem change list for problem (%s) must not be empty."
                    .formatted(problemId));
        }
        var currentSolverStatus = solverStatus.get();
        if (currentSolverStatus == SolverStatus.NOT_SOLVING) {
            throw new IllegalStateException("Cannot add the problem changes (%s) because the solver job (%s) is not solving."
                    .formatted(problemChangeList, currentSolverStatus));
        }
        return bestSolutionHolder.addProblemChange(solver, problemChangeList);
    }

    @Override
    public void terminateEarly() {
        terminatedEarly.set(true);
        if (!solver.isSolving()) {
            LOGGER.debug("terminateEarly() has been called while the solver was not solving. Cancelling the job.");
            var future = finalBestSolutionFuture.get();
            if (future == null) { // We set this; we messed up.
                throw new IllegalStateException(
                        "Impossible state: the finalBestSolutionFuture is not set yet for problemId (%s)."
                                .formatted(problemId));
            }
            future.cancel(false);
            solvingTerminated();
            return;
        }
        try {
            solverStatusModifyingLock.lock();
            switch (solverStatus.get()) {
                case SOLVING_SCHEDULED:
                    var future = finalBestSolutionFuture.get();
                    if (future == null) { // We set this; we messed up.
                        throw new IllegalStateException(
                                "Impossible state: the finalBestSolutionFuture is not set yet for problemId (%s)."
                                        .formatted(problemId));
                    }
                    future.cancel(false);
                    solvingTerminated();
                    break;
                case SOLVING_ACTIVE:
                    // Indirectly triggers solvingTerminated()
                    // No need to cancel the finalBestSolutionFuture as it will finish normally.
                    solver.terminateEarly();
                    break;
                case NOT_SOLVING:
                    // Do nothing, solvingTerminated() already called
                    break;
                default:
                    throw new IllegalStateException("Unsupported solverStatus (%s).".formatted(solverStatus));
            }
            try {
                // Don't return until bestSolutionConsumer won't be called anymore
                terminatedLatch.await();
                var terminatedCorrectly = terminatedLatch.await(EARLY_TERMINATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                if (!terminatedCorrectly) {
                    LOGGER.warn("""
                            The terminateEarly() call did not complete within ({}) for problemId ({}).
                            The final best solution may not have been consumed yet. This is likely a bug.
                            Please report this issue to Timefold with details on how to reproduce it.""",
                            EARLY_TERMINATION_TIMEOUT, problemId);
                    solvingTerminated(); // Clean up the solver job as if everything went fine, to prevent resource leaks.
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warn("The terminateEarly() call is interrupted.", e);
            }
        } finally {
            solverStatusModifyingLock.unlock();
        }
    }

    @Override
    public boolean isTerminatedEarly() {
        return terminatedEarly.get();
    }

    @Override
    public @NonNull Solution_ getFinalBestSolution() throws InterruptedException, ExecutionException {
        try {
            var future = finalBestSolutionFuture.get();
            if (future == null) { // We set this; we messed up.
                throw new IllegalStateException(
                        "Impossible state: the finalBestSolutionFuture is not set yet for problemId (%s)."
                                .formatted(problemId));
            }
            return future.get();
        } catch (CancellationException cancellationException) {
            LOGGER.debug(
                    "terminateEarly() has been called before the solver job started solving. Retrieving the input problem instead.");
            return problemFinder.apply(problemId);
        }
    }

    @Override
    public @NonNull Duration getSolvingDuration() {
        return Duration.ofMillis(solver.getTimeMillisSpent());
    }

    @Override
    public long getScoreCalculationCount() {
        return solver.getScoreCalculationCount();
    }

    @Override
    public long getMoveEvaluationCount() {
        return solver.getMoveEvaluationCount();
    }

    @Override
    public long getScoreCalculationSpeed() {
        return solver.getScoreCalculationSpeed();
    }

    @Override
    public long getMoveEvaluationSpeed() {
        return solver.getMoveEvaluationSpeed();
    }

    @Override
    public @NonNull ProblemSizeStatistics getProblemSizeStatistics() {
        var solverScope = solver.getSolverScope();
        var problemSizeStatistics = solverScope.getProblemSizeStatistics();
        if (problemSizeStatistics != null) {
            temporaryProblemSizeStatistics.set(null);
            return problemSizeStatistics;
        }
        // Solving has not started yet; we do not have a working solution.
        // Therefore we cannot rely on ScoreDirector's ValueRangeManager
        // and we need to use a new cold instance.
        // This will be inefficient on account of recomputing all the value ranges,
        // but it only exists to solve a corner case of accessing the problem size statistics
        // before the solving has started.
        // Once the solving has started, the problem size statistics will be computed
        // using the ScoreDirector's hot ValueRangeManager.
        return temporaryProblemSizeStatistics.updateAndGet(oldStatistics -> {
            if (oldStatistics != null) {
                // If the problem size statistics were already computed, return them.
                // This can happen if the problem size statistics were computed before the solving started.
                return oldStatistics;
            }
            var solutionDescriptor = solverScope.getSolutionDescriptor();
            var valueManager = ValueRangeManager.of(solutionDescriptor, problemFinder.apply(problemId));
            return valueManager.getProblemSizeStatistics();
        });
    }

    public SolverTermination<Solution_> getSolverTermination() {
        return solver.globalTermination;
    }

    void close() {
        var currentConsumerSupport = consumerSupport.getAndSet(null);
        if (currentConsumerSupport != null) {
            currentConsumerSupport.close();
        }
    }

    /**
     * A listener that unlocks the solverStatusModifyingLock when Solving has started.
     *
     * It prevents the following scenario caused by unlocking before Solving started:
     *
     * Thread 1:
     * solverStatusModifyingLock.unlock()
     * >solver.solve(...) // executes second
     *
     * Thread 2:
     * case SOLVING_ACTIVE:
     * >solver.terminateEarly(); // executes first
     *
     * The solver.solve() call resets the terminateEarly flag, and thus the solver will not be terminated
     * by the call, which means terminatedLatch will not be decremented, causing Thread 2 to wait forever
     * (at least until another Thread calls terminateEarly again).
     *
     * To prevent Thread 2 from potentially waiting forever, we only unlock the lock after the
     * solvingStarted phase lifecycle event is fired, meaning the terminateEarly flag will not be
     * reset and thus the solver will actually terminate.
     */
    private final class UnlockLockPhaseLifecycleListener extends PhaseLifecycleListenerAdapter<Solution_> {
        @Override
        public void solvingStarted(SolverScope<Solution_> solverScope) {
            // The solvingStarted event can be emitted as a result of addProblemChange().
            if (solverStatusModifyingLock.isLocked()) {
                solverStatusModifyingLock.unlock();
            }
        }
    }

    /**
     * A listener that consumes the solution from a phase only if the phase first initializes the solution.
     */
    private final class FirstInitializedSolutionPhaseLifecycleListener extends PhaseLifecycleListenerAdapter<Solution_> {

        private final ConsumerSupport<Solution_, ProblemId_> consumerSupport;

        public FirstInitializedSolutionPhaseLifecycleListener(ConsumerSupport<Solution_, ProblemId_> consumerSupport) {
            this.consumerSupport = consumerSupport;
        }

        @Override
        public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
            var eventPhase = solver.getPhaseList().stream()
                    .filter(phase -> ((AbstractPhase<Solution_>) phase).getPhaseIndex() == phaseScope.getPhaseIndex())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Impossible state: Solving failed for problemId (%s) because the phase id %d was not found."
                                    .formatted(problemId, phaseScope.getPhaseIndex())));
            if (eventPhase instanceof PossiblyInitializingPhase<Solution_> possiblyInitializingPhase
                    && possiblyInitializingPhase.isLastInitializingPhase()) {
                // The Solver thread calls the method,
                // but the consumption is done asynchronously by the Consumer thread.
                // Only happens if the phase initializes the solution.
                consumerSupport.consumeFirstInitializedSolution(phaseScope.getWorkingSolution(),
                        phaseScope.getPhaseId(),
                        possiblyInitializingPhase.getTerminationStatus().early());
            }
        }
    }

    /**
     * A listener that is triggered once when the solver starts the solving process.
     */
    private final class StartSolverJobPhaseLifecycleListener extends PhaseLifecycleListenerAdapter<Solution_> {

        private final ConsumerSupport<Solution_, ProblemId_> consumerSupport;

        public StartSolverJobPhaseLifecycleListener(ConsumerSupport<Solution_, ProblemId_> consumerSupport) {
            this.consumerSupport = consumerSupport;
        }

        @Override
        public void solvingStarted(SolverScope<Solution_> solverScope) {
            consumerSupport.consumeStartSolverJob(solverScope.getWorkingSolution());
        }
    }
}
