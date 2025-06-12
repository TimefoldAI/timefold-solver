package ai.timefold.solver.core.impl.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.api.solver.change.ProblemChangeDirector;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.testdomain.TestdataEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BestSolutionHolderTest {

    @Test
    void setBestSolution() {
        BestSolutionHolder<TestdataSolution> bestSolutionHolder = new BestSolutionHolder<>();
        assertThat(bestSolutionHolder.take()).isNull();

        TestdataSolution solution1 = TestdataSolution.generateSolution();
        TestdataSolution solution2 = TestdataSolution.generateSolution();

        bestSolutionHolder.set(solution1, () -> true);
        assertThat(bestSolutionHolder.take().getBestSolution()).isSameAs(solution1);
        assertThat(bestSolutionHolder.take()).isNull();

        bestSolutionHolder.set(solution1, () -> true);
        bestSolutionHolder.set(solution2, () -> false);
        assertThat(bestSolutionHolder.take().getBestSolution()).isSameAs(solution1);

        bestSolutionHolder.set(solution1, () -> true);
        bestSolutionHolder.set(solution2, () -> true);
        assertThat(bestSolutionHolder.take().getBestSolution()).isSameAs(solution2);
    }

    @Test
    void completeProblemChanges() {
        BestSolutionHolder<TestdataSolution> bestSolutionHolder = new BestSolutionHolder<>();

        CompletableFuture<Void> problemChange1 = addProblemChange(bestSolutionHolder);
        bestSolutionHolder.set(TestdataSolution.generateSolution(), () -> true);
        CompletableFuture<Void> problemChange2 = addProblemChange(bestSolutionHolder);

        bestSolutionHolder.take().completeProblemChanges();
        assertThat(problemChange1).isCompleted();
        assertThat(problemChange2).isNotCompleted();

        CompletableFuture<Void> problemChange3 = addProblemChange(bestSolutionHolder);
        bestSolutionHolder.set(TestdataSolution.generateSolution(), () -> true);
        bestSolutionHolder.set(TestdataSolution.generateSolution(), () -> true);
        CompletableFuture<Void> problemChange4 = addProblemChange(bestSolutionHolder);

        bestSolutionHolder.take().completeProblemChanges();

        assertThat(problemChange2).isCompleted();
        assertThat(problemChange3).isCompleted();
        assertThat(problemChange4).isNotCompleted();
    }

    @Test
    void cancelPendingChanges_noChangesRetrieved() {
        BestSolutionHolder<TestdataSolution> bestSolutionHolder = new BestSolutionHolder<>();

        CompletableFuture<Void> problemChange = addProblemChange(bestSolutionHolder);
        bestSolutionHolder.set(TestdataSolution.generateSolution(), () -> true);

        bestSolutionHolder.cancelPendingChanges();

        BestSolutionContainingProblemChanges<TestdataSolution> bestSolution = bestSolutionHolder.take();
        bestSolution.completeProblemChanges();

        assertThat(problemChange).isCancelled();
    }

    private CompletableFuture<Void> addProblemChange(BestSolutionHolder<TestdataSolution> bestSolutionHolder) {
        Solver<TestdataSolution> solver = mock(Solver.class);
        ProblemChange<TestdataSolution> problemChange = mock(ProblemChange.class);
        CompletableFuture<Void> futureChange = bestSolutionHolder.addProblemChange(solver, List.of(problemChange));
        verify(solver, times(1)).addProblemChanges(
                Mockito.argThat(problemChanges -> problemChanges.size() == 1 && problemChanges.get(0) == problemChange));
        return futureChange;
    }

    @RepeatedTest(value = 10, failureThreshold = 1) // Run it multiple times to increase the chance of catching a concurrency issue.
    void problemChangeBarrageIntermediateBestSolutionConsumer() throws InterruptedException {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestdataEasyScoreCalculator.class);

        var futureList = new ArrayList<RecordedFuture>();
        var executorService = Executors.newFixedThreadPool(2);
        try (var solverManager = SolverManager.<TestdataSolution, UUID> create(solverConfig, new SolverManagerConfig())) {
            var solverStartedLatch = new CountDownLatch(1);
            var solution = TestdataSolution.generateSolution();
            var solverJob = solverManager.solveBuilder()
                    .withProblemId(UUID.randomUUID())
                    .withProblem(solution)
                    .withFirstInitializedSolutionConsumer((testdataSolution, isTerminatedEarly) -> {
                        solverStartedLatch.countDown();
                    })
                    .withBestSolutionConsumer(testdataSolution -> {
                        // No need to do anything.
                    })
                    .run();
            solverStartedLatch.await(); // Only start adding problem changes after CH finished.

            var random = new Random(0);
            var problemChangeCount = 200; // Arbitrary, for a reasonable test duration.
            var problemChangesAddedLatch = new CountDownLatch(problemChangeCount);
            for (int i = 0; i < problemChangeCount; i++) {
                // Emulate a random delay between problem changes, as it would happen in real world.
                var randomDelayNanos = random.nextInt(1_000_000);
                var start = System.nanoTime();
                while ((System.nanoTime() - randomDelayNanos) < start) {
                    Thread.onSpinWait();
                }
                // Submit the problem change and store the future.
                var problemChange = random.nextBoolean()
                        ? new EntityAddingProblemChange(problemChangesAddedLatch)
                        : new EntityRemovingProblemChange(problemChangesAddedLatch);
                futureList.add(new RecordedFuture(i, solverJob.addProblemChange(problemChange)));
            }
            // All problem changes have been added.
            // Does not guarantee all have been processed though.
            problemChangesAddedLatch.await();

            // A best solution should have been produced for all the processed changes.
            // Any incomplete futures here means some problem change was "lost".
            var lostFutureList = futureList.stream()
                    .filter(future -> {
                        await().atMost(Duration.ofSeconds(1))
                                .pollInterval(Duration.ofMillis(1))
                                .until(future::isDone);
                        return !future.isDone();
                    })
                    .toList();
            var lostFutureCount = lostFutureList.size();
            if (lostFutureCount == 0) {
                return;
            }
            // The only exception to the rule:
            // the very last problem changes, which might not have been processed yet
            // by the time the solver was forced to terminate.
            var minIncompleteFutureId = lostFutureList.stream()
                    .mapToInt(f -> f.id)
                    .min()
                    .orElseThrow(() -> new AssertionError("Impossible state: no incomplete future found."));
            assertThat(minIncompleteFutureId).isEqualTo(problemChangeCount - lostFutureCount);
        } finally {
            executorService.shutdownNow();
            // The solver is terminated.
            // All incomplete futures should have been canceled.
            var incompleteFutureList = futureList.stream()
                    .filter(future -> {
                        await().atMost(Duration.ofSeconds(1))
                                .pollInterval(Duration.ofMillis(1))
                                .until(future::isDone);
                        return !future.isDone();
                    })
                    .toList();
            assertThat(incompleteFutureList)
                    .as("All futures should have been completed by now.")
                    .isEmpty();
        }

    }

    private record RecordedFuture(int id, CompletableFuture<Void> future) {

        boolean isDone() {
            return future.isDone();
        }

    }

    @NullMarked
    private record EntityAddingProblemChange(CountDownLatch latch) implements ProblemChange<TestdataSolution> {

        @Override
        public void doChange(TestdataSolution workingSolution, ProblemChangeDirector problemChangeDirector) {
            var entity = new TestdataEntity(UUID.randomUUID().toString());
            problemChangeDirector.addEntity(entity,
                    e -> workingSolution.getEntityList().add(e));
            problemChangeDirector.updateShadowVariables();
            latch.countDown();
        }

    }

    @NullMarked
    private record EntityRemovingProblemChange(CountDownLatch latch) implements ProblemChange<TestdataSolution> {

        @Override
        public void doChange(TestdataSolution workingSolution, ProblemChangeDirector problemChangeDirector) {
            if (workingSolution.getEntityList().size() < 2) {
                latch.countDown();
                return;
            }
            var entity = workingSolution.getEntityList().get(0);
            problemChangeDirector.removeEntity(entity,
                    e -> workingSolution.getEntityList().remove(e));
            problemChangeDirector.updateShadowVariables();
            latch.countDown();
        }

    }

}
