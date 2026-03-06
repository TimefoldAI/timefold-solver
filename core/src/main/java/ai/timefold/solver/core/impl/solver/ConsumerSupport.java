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

/**
 * Support class for consuming solver events in a separate thread.
 * It ensures that the events are consumed in the correct order and handles exceptions properly.
 * <p>
 * The public consume* methods in this class are called by the Solver thread,
 * and the actual consumption of events is scheduled in the schedule* to a separate Consumer thread
 * produced by the {@link #consumerExecutor}.
 * The consumptions are protected by semaphores to ensure the correct order and to avoid concurrent consumptions,
 * and it is the responsibility of the consume* methods to only run the schedule* methods when locked.
 *
 * @param <Solution_> the solution type
 * @param <ProblemId_> the problem id type
 */
@NullMarked
final class ConsumerSupport<Solution_, ProblemId_> implements AutoCloseable {

    private final ProblemId_ problemId;
    private final Consumer<NewBestSolutionEvent<Solution_>> bestSolutionConsumer;
    private final Consumer<FinalBestSolutionEvent<Solution_>> finalBestSolutionConsumer;
    private final Consumer<FirstInitializedSolutionEvent<Solution_>> firstInitializedSolutionConsumer;
    private final Consumer<SolverJobStartedEvent<Solution_>> solverJobStartedConsumer;
    private final BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler;
    private final Semaphore activeConsumption = new Semaphore(1);
    private final Semaphore firstSolutionConsumption = new Semaphore(1);
    private final Semaphore startSolverJobConsumption = new Semaphore(1);
    private final BestSolutionHolder<Solution_> bestSolutionHolder;
    private final ExecutorService consumerExecutor = Executors.newSingleThreadExecutor();
    private final AtomicReference<Solution_> firstInitializedSolution = new AtomicReference<>();
    private final AtomicReference<Solution_> initialSolution = new AtomicReference<>();

    public ConsumerSupport(ProblemId_ problemId,
            Consumer<NewBestSolutionEvent<Solution_>> bestSolutionConsumer,
            Consumer<FinalBestSolutionEvent<Solution_>> finalBestSolutionConsumer,
            Consumer<FirstInitializedSolutionEvent<Solution_>> firstInitializedSolutionConsumer,
            Consumer<SolverJobStartedEvent<Solution_>> solverJobStartedConsumer,
            BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler,
            BestSolutionHolder<Solution_> bestSolutionHolder) {
        this.problemId = problemId;
        this.bestSolutionConsumer = bestSolutionConsumer;
        this.finalBestSolutionConsumer = finalBestSolutionConsumer == null ? finalBestSolution -> {
        } : finalBestSolutionConsumer;
        this.firstInitializedSolutionConsumer =
                firstInitializedSolutionConsumer == null ? event -> {
                } : firstInitializedSolutionConsumer;
        this.solverJobStartedConsumer = solverJobStartedConsumer;
        this.exceptionHandler = exceptionHandler;
        this.bestSolutionHolder = bestSolutionHolder;
    }

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
                    .whenCompleteAsync((solution, throwable) -> {
                        activeConsumption.release();
                        tryConsumeWaitingIntermediateBestSolution();
                    }, consumerExecutor);
        }
    }

    private CompletableFuture<Void> scheduleIntermediateBestSolutionConsumption() {
        return CompletableFuture.runAsync(() -> {
            var bestSolutionContainingProblemChanges = bestSolutionHolder.take();
            if (bestSolutionContainingProblemChanges != null) {
                try {
                    if (bestSolutionConsumer != null) {
                        var event = new NewBestSolutionEventImpl<>(bestSolutionContainingProblemChanges.getBestSolution(),
                                bestSolutionContainingProblemChanges.getProducerId());
                        bestSolutionConsumer.accept(event);
                    }
                    bestSolutionContainingProblemChanges.completeProblemChanges();
                } catch (Throwable throwable) {
                    if (exceptionHandler != null) {
                        exceptionHandler.accept(problemId, throwable);
                    }
                    bestSolutionContainingProblemChanges.completeProblemChangesExceptionally(throwable);
                }
            }
        }, consumerExecutor);
    }

    void consumeFirstInitializedSolution(Solution_ solution, EventProducerId producerId, boolean isTerminatedEarly) {
        try { // During the solving process, this lock is called once, and it won't block the Solver thread
            firstSolutionConsumption.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted when waiting for the first initialized solution consumption.");
        }
        this.firstInitializedSolution.getAndSet(solution); // Reachable more than once; problem change triggers restart.
        scheduleFirstInitializedSolutionConsumption(s -> firstInitializedSolutionConsumer
                .accept(new FirstInitializedSolutionEventImpl<>(s, producerId, isTerminatedEarly)))
                .whenCompleteAsync((unused, throwable) -> firstSolutionConsumption.release(), consumerExecutor);
    }

    private CompletableFuture<Void> scheduleFirstInitializedSolutionConsumption(Consumer<? super Solution_> solutionConsumer) {
        return scheduleConsumption(solutionConsumer, firstInitializedSolution.get());
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

    void consumeStartSolverJob(Solution_ solution) {
        try { // During the solving process, this lock is called once, and it won't block the Solver thread
            startSolverJobConsumption.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted when waiting for the start solver job consumption.");
        }
        this.initialSolution.getAndSet(solution); // Reachable more than once; problem change triggers restart.
        scheduleStartJobConsumption().whenCompleteAsync((unused, throwable) -> startSolverJobConsumption.release(),
                consumerExecutor);
    }

    private CompletableFuture<Void> scheduleStartJobConsumption() {
        return scheduleConsumption(
                solverJobStartedConsumer == null ? null
                        : solution -> solverJobStartedConsumer.accept(new SolverJobStartedEventImpl<>(solution)),
                initialSolution.get());
    }

    void consumeFinalBestSolution(Solution_ solution) { // Called on the Solver thread, after solving is finished.
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
        scheduleFinalBestSolutionConsumption(solution)
                .whenComplete((unused, throwable) -> releaseAll());
    }

    private CompletableFuture<Void> scheduleFinalBestSolutionConsumption(Solution_ solution) {
        return CompletableFuture.runAsync(() -> {
            try {
                finalBestSolutionConsumer.accept(new FinalBestSolutionEventImpl<>(solution));
            } catch (Throwable throwable) {
                if (exceptionHandler != null) {
                    exceptionHandler.accept(problemId, throwable);
                }
            }
        }, consumerExecutor)
                .whenComplete((unused, throwable) -> {
                    // If there is no intermediate best solution consumer, complete the problem changes now.
                    if (bestSolutionConsumer == null) {
                        var solutionHolder = bestSolutionHolder.take();
                        if (solutionHolder != null) {
                            solutionHolder.completeProblemChanges();
                        }
                    }
                    shutdownConsumerExecutor();
                });
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

    private void shutdownConsumerExecutor() {
        // Cancel problem changes that arrived after the solver terminated.
        bestSolutionHolder.cancelPendingChanges();
        consumerExecutor.shutdownNow();
    }

    @Override
    public void close() {
        if (consumerExecutor.isShutdown()) {
            return; // Already closed, do nothing.
        }
        try {
            acquireAll();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted when waiting for closing the consumer.");
        } finally {
            shutdownConsumerExecutor();
            releaseAll();
        }
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
