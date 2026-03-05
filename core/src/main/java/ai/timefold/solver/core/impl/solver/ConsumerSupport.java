package ai.timefold.solver.core.impl.solver;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.solver.event.EventProducerId;
import ai.timefold.solver.core.api.solver.event.FinalBestSolutionEvent;
import ai.timefold.solver.core.api.solver.event.FirstInitializedSolutionEvent;
import ai.timefold.solver.core.api.solver.event.NewBestSolutionEvent;
import ai.timefold.solver.core.api.solver.event.SolverJobStartedEvent;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class ConsumerSupport<Solution_, ProblemId_> implements AutoCloseable {

    private final ProblemId_ problemId;
    private final @Nullable Consumer<NewBestSolutionEvent<Solution_>> bestSolutionConsumer;
    private final Consumer<FinalBestSolutionEvent<Solution_>> finalBestSolutionConsumer;
    private final Consumer<FirstInitializedSolutionEvent<Solution_>> firstInitializedSolutionConsumer;
    private final @Nullable Consumer<SolverJobStartedEvent<Solution_>> solverJobStartedConsumer;
    private final BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler;
    private final Semaphore activeConsumption = new Semaphore(1);
    private final Semaphore firstSolutionConsumption = new Semaphore(1);
    private final Semaphore startSolverJobConsumption = new Semaphore(1);
    private final BestSolutionHolder<Solution_> bestSolutionHolder;
    private final ExecutorService consumerExecutor = Executors.newSingleThreadExecutor();
    private final AtomicReference<@Nullable Solution_> firstInitializedSolution = new AtomicReference<>();
    private final AtomicReference<@Nullable Solution_> initialSolution = new AtomicReference<>();

    public ConsumerSupport(ProblemId_ problemId, @Nullable Consumer<NewBestSolutionEvent<Solution_>> bestSolutionConsumer,
            @Nullable Consumer<FinalBestSolutionEvent<Solution_>> finalBestSolutionConsumer,
            @Nullable Consumer<FirstInitializedSolutionEvent<Solution_>> firstInitializedSolutionConsumer,
            @Nullable Consumer<SolverJobStartedEvent<Solution_>> solverJobStartedConsumer,
            BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler,
            BestSolutionHolder<Solution_> bestSolutionHolder) {
        this.problemId = problemId;
        this.bestSolutionConsumer = bestSolutionConsumer;
        this.finalBestSolutionConsumer = finalBestSolutionConsumer == null ? finalBestSolution -> {
        } : finalBestSolutionConsumer;
        this.firstInitializedSolutionConsumer = firstInitializedSolutionConsumer == null ? event -> {
        } : firstInitializedSolutionConsumer;
        this.solverJobStartedConsumer = solverJobStartedConsumer;
        this.exceptionHandler = exceptionHandler;
        this.bestSolutionHolder = bestSolutionHolder;
    }

    // Called on the Solver thread.
    void consumeIntermediateBestSolution(Solution_ solution, EventProducerId producerId,
            BooleanSupplier isEveryProblemChangeProcessed) {
        /*
         * If the bestSolutionConsumer is not provided, the best solution is still set for the purpose of recording
         * problem changes.
         */
        bestSolutionHolder.set(solution, producerId, isEveryProblemChangeProcessed);
        if (bestSolutionConsumer != null) {
            tryConsumeWaitingIntermediateBestSolution();
        }
    }

    // Called both on the Solver thread and the Consumer thread.
    private void tryConsumeWaitingIntermediateBestSolution() {
        if (bestSolutionHolder.isEmpty()) {
            return; // There is no best solution to consume.
        }
        if (activeConsumption.tryAcquire()) {
            scheduleIntermediateBestSolutionConsumption()
                    .whenComplete((solution, throwable) -> activeConsumption.release())
                    .thenRunAsync(this::tryConsumeWaitingIntermediateBestSolution, consumerExecutor);
        }
    }

    /**
     * Called both on the Solver thread and the Consumer thread.
     * Don't call without locking, otherwise multiple consumptions may be scheduled.
     * 
     * @return future which completes when the consumption is done; can be unlocked then
     */
    private CompletableFuture<Void> scheduleIntermediateBestSolutionConsumption() {
        return CompletableFuture.runAsync(() -> {
            BestSolutionContainingProblemChanges<Solution_> bestSolutionContainingProblemChanges = bestSolutionHolder.take();
            if (bestSolutionContainingProblemChanges != null) {
                try {
                    if (bestSolutionConsumer != null) {
                        bestSolutionConsumer
                                .accept(new NewBestSolutionEventImpl<>(bestSolutionContainingProblemChanges.getBestSolution(),
                                        bestSolutionContainingProblemChanges.getProducerId()));
                    }
                    bestSolutionContainingProblemChanges.completeProblemChanges();
                } catch (Throwable throwable) {
                    exceptionHandler.accept(problemId, throwable);
                    bestSolutionContainingProblemChanges.completeProblemChangesExceptionally(throwable);
                }
            }
        }, consumerExecutor);
    }

    // Called on the Solver thread.
    void consumeFirstInitializedSolution(Solution_ solution, EventProducerId producerId, boolean isTerminatedEarly) {
        try {
            // Called on the solver thread
            // During the solving process, this lock is called once, and it won't block the Solver thread
            firstSolutionConsumption.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted when waiting for the first initialized solution consumption.");
        }
        // called on the Consumer thread
        this.firstInitializedSolution.getAndSet(solution); // Reachable more than once; problem change triggers restart.
        scheduleFirstInitializedSolutionConsumption(s -> firstInitializedSolutionConsumer
                .accept(new FirstInitializedSolutionEventImpl<>(s, producerId, isTerminatedEarly)))
                .whenComplete((unused, throwable) -> firstSolutionConsumption.release());
    }

    // Called on the consumer thread
    void consumeStartSolverJob(Solution_ solution) {
        try {
            // Called on the solver thread
            // During the solving process, this lock is called once, and it won't block the Solver thread
            startSolverJobConsumption.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted when waiting for the start solver job consumption.");
        }
        // called on the Consumer thread
        this.initialSolution.getAndSet(solution); // Reachable more than once; problem change triggers restart.
        scheduleStartJobConsumption().whenComplete((unused, throwable) -> startSolverJobConsumption.release());
    }

    // Called on the Solver thread after Solver#solve() returns.
    void consumeFinalBestSolution(Solution_ solution) {
        try {
            acquireAll();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted when waiting for the final best solution consumption.");
        }
        // Make sure the final best solution is consumed by the intermediate best solution consumer first.
        // Situation:
        // The consumer is consuming the last but one best solution. The final best solution is waiting for the consumer.
        if (bestSolutionConsumer != null) {
            scheduleIntermediateBestSolutionConsumption();
        }
        scheduleBestSolutionConsumption(solution)
                .whenComplete((unused, throwable) -> releaseAll());
    }

    private CompletableFuture<Void> scheduleBestSolutionConsumption(Solution_ solution) {
        return CompletableFuture.runAsync(() -> {
            try {
                finalBestSolutionConsumer.accept(new FinalBestSolutionEventImpl<>(solution));
            } catch (Throwable throwable) {
                exceptionHandler.accept(problemId, throwable);
            } finally {
                // If there is no intermediate best solution consumer, complete the problem changes now.
                if (bestSolutionConsumer == null) {
                    var solutionHolder = bestSolutionHolder.take();
                    if (solutionHolder != null) {
                        solutionHolder.completeProblemChanges();
                    }
                }
                // Cancel problem changes that arrived after the solver terminated.
                bestSolutionHolder.cancelPendingChanges();
                disposeConsumerThread();
            }
        }, consumerExecutor);
    }

    /**
     * Called on the Consumer thread.
     * Don't call without locking firstSolutionConsumption,
     * because the consumption may not be executed before the final best solution is executed.
     * 
     * @return future which completes when the consumption is done; can be unlocked then
     */
    private CompletableFuture<Void> scheduleFirstInitializedSolutionConsumption(Consumer<? super Solution_> solutionConsumer) {
        return scheduleConsumption(solutionConsumer, firstInitializedSolution.get());
    }

    /**
     * Called on the Consumer thread.
     * Don't call without locking startSolverJobConsumption,
     * because the consumption may not be executed before the final best solution is executed.
     * 
     * @return future which completes when the consumption is done; can be unlocked then
     */
    private CompletableFuture<Void> scheduleStartJobConsumption() {
        return scheduleConsumption(
                solverJobStartedConsumer == null ? null
                        : solution -> solverJobStartedConsumer.accept(new SolverJobStartedEventImpl<>(solution)),
                initialSolution.get());
    }

    /**
     * Assumes that it runs locked.
     * 
     * @return future which completes when the consumption is done; can be unlocked then
     */
    private CompletableFuture<Void> scheduleConsumption(@Nullable Consumer<? super Solution_> consumer,
            @Nullable Solution_ solution) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (consumer != null && solution != null) {
                    consumer.accept(solution);
                }
            } catch (Throwable throwable) {
                exceptionHandler.accept(problemId, throwable);
            }
        }, consumerExecutor);
    }

    private void acquireAll() throws InterruptedException {
        // Wait for the previous consumption to complete.
        // As the solver has already finished, holding the solver thread is not an issue.
        activeConsumption.acquire();
        // Wait for the start job event to complete
        startSolverJobConsumption.acquire();
        // Wait for the first solution consumption to complete
        firstSolutionConsumption.acquire();
    }

    private void releaseAll() {
        activeConsumption.release();
        startSolverJobConsumption.release();
        firstSolutionConsumption.release();
    }

    @Override
    public void close() {
        try {
            acquireAll();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted when waiting for closing the consumer.");
        } finally {
            disposeConsumerThread();
            bestSolutionHolder.cancelPendingChanges();
            releaseAll();
        }
    }

    private void disposeConsumerThread() {
        consumerExecutor.shutdownNow();
    }

    record NewBestSolutionEventImpl<Solution_>(Solution_ solution,
            EventProducerId producerId) implements NewBestSolutionEvent<Solution_> {
    }

    record FirstInitializedSolutionEventImpl<Solution_>(Solution_ solution, EventProducerId producerId,
            boolean isTerminatedEarly) implements FirstInitializedSolutionEvent<Solution_> {
    }

    record FinalBestSolutionEventImpl<Solution_>(Solution_ solution) implements FinalBestSolutionEvent<Solution_> {
    }

    record SolverJobStartedEventImpl<Solution_>(Solution_ solution) implements SolverJobStartedEvent<Solution_> {
    }
}
