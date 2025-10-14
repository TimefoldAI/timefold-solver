package ai.timefold.solver.core.impl.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.api.solver.change.ProblemChangeDirector;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.testdomain.TestdataEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.awaitility.pollinterval.FibonacciPollInterval;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.RepeatedTest;

class ProblemChangeBarrageIT {

    // Run it multiple times to increase the chance of catching a concurrency issue.
    // Also run it as integration test, away from the other solver tests which may be executing in parallel,
    // possibly causing contention on shared resources.
    @RepeatedTest(value = 10, failureThreshold = 1)
    void problemChangeBarrageIntermediateBestSolutionConsumer() throws InterruptedException {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestdataEasyScoreCalculator.class);

        var futureList = new ArrayList<RecordedFuture>();
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
            var lostFutureList = getOutstandingFutures(futureList);
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
            // The solver is terminated.
            // All incomplete futures should have been canceled.
            var incompleteFutureList = getOutstandingFutures(futureList);
            assertThat(incompleteFutureList)
                    .as("All futures should have been completed by now.")
                    .isEmpty();
        }

    }

    private static List<RecordedFuture> getOutstandingFutures(List<RecordedFuture> futureList) {
        // We wait for at most 10 seconds for each future to complete,
        // but we expect the completion to be done almost immediately.
        // The large timeout is just to avoid test flakes on congested environments;
        // after such a long time, something is clearly wrong.
        return futureList.stream()
                .filter(future -> {
                    await().atMost(Duration.ofSeconds(10))
                            .pollInterval(FibonacciPollInterval.fibonacci(1, TimeUnit.MILLISECONDS))
                            .until(future::isDone);
                    return !future.isDone();
                })
                .toList();
    }

    @NullMarked
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
