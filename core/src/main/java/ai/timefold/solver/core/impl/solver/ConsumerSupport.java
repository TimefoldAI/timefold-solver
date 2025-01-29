package ai.timefold.solver.core.impl.solver;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.solver.SolverJobBuilder.FirstInitializedSolutionConsumer;

final class ConsumerSupport<Solution_, ProblemId_> implements AutoCloseable {

    private final ProblemId_ problemId;
    private final Consumer<? super Solution_> bestSolutionConsumer;
    private final Consumer<? super Solution_> finalBestSolutionConsumer;
    private final FirstInitializedSolutionConsumer<? super Solution_> firstInitializedSolutionConsumer;
    private final Consumer<? super Solution_> solverJobStartedConsumer;
    private final BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler;
    private final Semaphore activeConsumption = new Semaphore(1);
    private final Semaphore firstSolutionConsumption = new Semaphore(1);
    private final Semaphore startSolverJobConsumption = new Semaphore(1);
    private final BestSolutionHolder<Solution_> bestSolutionHolder;
    private final ExecutorService consumerExecutor = Executors.newSingleThreadExecutor();
    private Solution_ firstInitializedSolution;
    private Solution_ initialSolution;

    public ConsumerSupport(ProblemId_ problemId, Consumer<? super Solution_> bestSolutionConsumer,
            Consumer<? super Solution_> finalBestSolutionConsumer,
            FirstInitializedSolutionConsumer<? super Solution_> firstInitializedSolutionConsumer,
            Consumer<? super Solution_> solverJobStartedConsumer,
            BiConsumer<? super ProblemId_, ? super Throwable> exceptionHandler,
            BestSolutionHolder<Solution_> bestSolutionHolder) {
        this.problemId = problemId;
        this.bestSolutionConsumer = bestSolutionConsumer;
        this.finalBestSolutionConsumer = finalBestSolutionConsumer == null ? finalBestSolution -> {
        } : finalBestSolutionConsumer;
        this.firstInitializedSolutionConsumer = firstInitializedSolutionConsumer == null ? (solution, isTerminatedEarly) -> {
        } : firstInitializedSolutionConsumer;
        this.solverJobStartedConsumer = solverJobStartedConsumer;
        this.exceptionHandler = exceptionHandler;
        this.bestSolutionHolder = bestSolutionHolder;
        this.firstInitializedSolution = null;
        this.initialSolution = null;
    }

    // Called on the Solver thread.
    void consumeIntermediateBestSolution(Solution_ bestSolution, BooleanSupplier isEveryProblemChangeProcessed) {
        /*
         * If the bestSolutionConsumer is not provided, the best solution is still set for the purpose of recording
         * problem changes.
         */
        bestSolutionHolder.set(bestSolution, isEveryProblemChangeProcessed);
        if (bestSolutionConsumer != null) {
            tryConsumeWaitingIntermediateBestSolution();
        }
    }

    // Called on the Solver thread.
    void consumeFirstInitializedSolution(Solution_ firstInitializedSolution, boolean isTerminatedEarly) {
        try {
            // Called on the solver thread
            // During the solving process, this lock is called once, and it won't block the Solver thread
            firstSolutionConsumption.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted when waiting for the first initialized solution consumption.");
        }
        // called on the Consumer thread
        this.firstInitializedSolution = firstInitializedSolution;
        scheduleFirstInitializedSolutionConsumption(
                solution -> firstInitializedSolutionConsumer.accept(solution, isTerminatedEarly));
    }

    // Called on the consumer thread
    void consumeStartSolverJob(Solution_ initialSolution) {
        try {
            // Called on the solver thread
            // During the solving process, this lock is called once, and it won't block the Solver thread
            startSolverJobConsumption.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted when waiting for the start solver job consumption.");
        }
        // called on the Consumer thread
        this.initialSolution = initialSolution;
        scheduleStartJobConsumption();
    }

    // Called on the Solver thread after Solver#solve() returns.
    void consumeFinalBestSolution(Solution_ finalBestSolution) {
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
        consumerExecutor.submit(() -> {
            try {
                finalBestSolutionConsumer.accept(finalBestSolution);
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
                releaseAll();
                disposeConsumerThread();
            }
        });
    }

    // Called both on the Solver thread and the Consumer thread.
    private void tryConsumeWaitingIntermediateBestSolution() {
        if (bestSolutionHolder.isEmpty()) {
            return; // There is no best solution to consume.
        }
        if (activeConsumption.tryAcquire()) {
            scheduleIntermediateBestSolutionConsumption().thenRunAsync(this::tryConsumeWaitingIntermediateBestSolution,
                    consumerExecutor);
        }
    }

    /**
     * Called both on the Solver thread and the Consumer thread.
     * Don't call without locking, otherwise multiple consumptions may be scheduled.
     */
    private CompletableFuture<Void> scheduleIntermediateBestSolutionConsumption() {
        return CompletableFuture.runAsync(() -> {
            BestSolutionContainingProblemChanges<Solution_> bestSolutionContainingProblemChanges = bestSolutionHolder.take();
            if (bestSolutionContainingProblemChanges != null) {
                try {
                    bestSolutionConsumer.accept(bestSolutionContainingProblemChanges.getBestSolution());
                    bestSolutionContainingProblemChanges.completeProblemChanges();
                } catch (Throwable throwable) {
                    if (exceptionHandler != null) {
                        exceptionHandler.accept(problemId, throwable);
                    }
                    bestSolutionContainingProblemChanges.completeProblemChangesExceptionally(throwable);
                } finally {
                    activeConsumption.release();
                }
            }
        }, consumerExecutor);
    }

    /**
     * Called on the Consumer thread.
     * Don't call without locking firstSolutionConsumption,
     * because the consumption may not be executed before the final best solution is executed.
     */
    private void scheduleFirstInitializedSolutionConsumption(Consumer<? super Solution_> firstInitializedSolutionConsumer) {
        scheduleConsumption(firstSolutionConsumption, firstInitializedSolutionConsumer, firstInitializedSolution);
    }

    /**
     * Called on the Consumer thread.
     * Don't call without locking startSolverJobConsumption,
     * because the consumption may not be executed before the final best solution is executed.
     */
    private void scheduleStartJobConsumption() {
        scheduleConsumption(startSolverJobConsumption, solverJobStartedConsumer, initialSolution);
    }

    private void scheduleConsumption(Semaphore semaphore, Consumer<? super Solution_> consumer, Solution_ solution) {
        CompletableFuture.runAsync(() -> {
            try {
                if (consumer != null && solution != null) {
                    consumer.accept(solution);
                }
            } catch (Throwable throwable) {
                if (exceptionHandler != null) {
                    exceptionHandler.accept(problemId, throwable);
                }
            } finally {
                semaphore.release();
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
}
