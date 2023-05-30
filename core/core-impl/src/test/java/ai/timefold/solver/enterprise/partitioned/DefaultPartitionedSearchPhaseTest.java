package ai.timefold.solver.enterprise.partitioned;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.partitionedsearch.PartitionedSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.partitionedsearch.PartitionedSearchPhase;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.DefaultSolver;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;
import ai.timefold.solver.enterprise.partitioned.testdata.TestdataFaultyEntity;
import ai.timefold.solver.enterprise.partitioned.testdata.TestdataSolutionPartitioner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class DefaultPartitionedSearchPhaseTest {

    @Test
    @Timeout(5)
    void partCount() {
        partCount(SolverConfig.MOVE_THREAD_COUNT_NONE);
    }

    @Test
    @Timeout(5)
    void partCountAndMoveThreadCount() {
        partCount("2");
    }

    void partCount(String moveThreadCount) {
        final int partSize = 3;
        final int partCount = 7;
        SolverFactory<TestdataSolution> solverFactory = createSolverFactory(false, moveThreadCount, partSize);
        DefaultSolver<TestdataSolution> solver = (DefaultSolver<TestdataSolution>) solverFactory.buildSolver();
        PartitionedSearchPhase<TestdataSolution> phase = (PartitionedSearchPhase<TestdataSolution>) solver.getPhaseList()
                .get(0);
        phase.addPhaseLifecycleListener(new PhaseLifecycleListenerAdapter<>() {
            @Override
            public void phaseStarted(AbstractPhaseScope<TestdataSolution> phaseScope) {
                assertThat(((PartitionedSearchPhaseScope) phaseScope).getPartCount()).isEqualTo(Integer.valueOf(partCount));
            }
        });
        solver.solve(createSolution(partCount * partSize, 2));
    }

    private static SolverFactory<TestdataSolution> createSolverFactory(boolean infinite, String moveThreadCount, int partSize) {
        SolverConfig solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        solverConfig.setMoveThreadCount(moveThreadCount);
        PartitionedSearchPhaseConfig partitionedSearchPhaseConfig = new PartitionedSearchPhaseConfig();
        partitionedSearchPhaseConfig.setSolutionPartitionerClass(TestdataSolutionPartitioner.class);
        Map<String, String> solutionPartitionerCustomProperties = new HashMap<>();
        solutionPartitionerCustomProperties.put("partSize", Integer.toString(partSize));
        partitionedSearchPhaseConfig.setSolutionPartitionerCustomProperties(solutionPartitionerCustomProperties);
        solverConfig.setPhaseConfigList(Arrays.asList(partitionedSearchPhaseConfig));
        ConstructionHeuristicPhaseConfig constructionHeuristicPhaseConfig = new ConstructionHeuristicPhaseConfig();
        LocalSearchPhaseConfig localSearchPhaseConfig = new LocalSearchPhaseConfig();
        if (!infinite) {
            localSearchPhaseConfig.setTerminationConfig(new TerminationConfig().withStepCountLimit(1));
        }
        partitionedSearchPhaseConfig.setPhaseConfigList(
                Arrays.asList(constructionHeuristicPhaseConfig, localSearchPhaseConfig));
        return SolverFactory.create(solverConfig);
    }

    private static TestdataSolution createSolution(int entities, int values) {
        TestdataSolution solution = new TestdataSolution();
        solution.setEntityList(IntStream.range(0, entities)
                .mapToObj(i -> new TestdataEntity(Character.toString((char) (65 + i))))
                .collect(Collectors.toList()));
        solution.setValueList(IntStream.range(0, values)
                .mapToObj(i -> new TestdataValue(Integer.toString(i)))
                .collect(Collectors.toList()));
        return solution;
    }

    @Test
    @Timeout(5)
    void exceptionPropagation() {
        final int partSize = 7;
        final int partCount = 3;

        TestdataSolution solution = createSolution(partCount * partSize - 1, 100);
        solution.getEntityList().add(new TestdataFaultyEntity("XYZ"));
        assertThat(solution.getEntityList().size()).isEqualTo(partSize * partCount);

        SolverFactory<TestdataSolution> solverFactory = createSolverFactory(false, SolverConfig.MOVE_THREAD_COUNT_NONE,
                partSize);
        Solver<TestdataSolution> solver = solverFactory.buildSolver();
        assertThatIllegalStateException()
                .isThrownBy(() -> solver.solve(solution))
                .withMessageMatching(".*partIndex.*Relayed.*")
                .withRootCauseExactlyInstanceOf(TestdataFaultyEntity.TestException.class);
    }

    @Test
    @Timeout(5)
    void terminateEarly() throws InterruptedException, ExecutionException {
        final int partSize = 1;
        final int partCount = 2;

        TestdataSolution solution = createSolution(partCount * partSize, 10);

        SolverFactory<TestdataSolution> solverFactory = createSolverFactory(true, SolverConfig.MOVE_THREAD_COUNT_NONE,
                partSize);
        Solver<TestdataSolution> solver = solverFactory.buildSolver();
        CountDownLatch solvingStarted = new CountDownLatch(1);
        ((DefaultSolver<TestdataSolution>) solver).addPhaseLifecycleListener(
                new PhaseLifecycleListenerAdapter<>() {
                    @Override
                    public void solvingStarted(SolverScope<TestdataSolution> solverScope) {
                        solvingStarted.countDown();
                    }
                });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<TestdataSolution> solutionFuture = executor.submit(() -> solver.solve(solution));

        // make sure solver has started solving before terminating early
        solvingStarted.await();
        assertThat(solver.terminateEarly()).isTrue();
        assertThat(solver.isTerminateEarly()).isTrue();

        executor.shutdown();
        assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue();
        assertThat(solutionFuture.get()).isNotNull();
    }

    @Test
    @Timeout(60)
    // All the credits to https://github.com/BobbyHirst.
    void solvePartitionedWithProblemChange() throws InterruptedException {
        // Create a partitioned daemon solver.
        SolverFactory<TestdataSolution> solverFactory = createSolverFactory(true, SolverConfig.MOVE_THREAD_COUNT_NONE,
                1);
        Solver<TestdataSolution> solver = solverFactory.buildSolver();

        final int valueCount = 4;
        TestdataSolution solution = TestdataSolution.generateSolution(valueCount, valueCount);

        AtomicReference<TestdataSolution> bestSolution = new AtomicReference<>();
        CountDownLatch solutionWithProblemChangeReceived = new CountDownLatch(1);

        CountDownLatch solvingStarted = new CountDownLatch(1);
        ((DefaultSolver<TestdataSolution>) solver).addPhaseLifecycleListener(
                new PhaseLifecycleListenerAdapter<>() {
                    @Override
                    public void solvingStarted(SolverScope<TestdataSolution> solverScope) {
                        solvingStarted.countDown();
                    }
                });

        solver.addEventListener(bestSolutionChangedEvent -> {
            if (bestSolutionChangedEvent.isEveryProblemChangeProcessed()) {
                TestdataSolution newBestSolution = bestSolutionChangedEvent.getNewBestSolution();
                if (newBestSolution.getValueList().size() == valueCount + 1) {
                    bestSolution.set(newBestSolution);
                    solutionWithProblemChangeReceived.countDown();
                }
            }
        });

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            executorService.submit(() -> solver.solve(solution));

            solvingStarted.await(); // Make sure we submit a ProblemChange only after the Solver started solving.
            solver.addProblemChange((workingSolution, problemChangeDirector) -> problemChangeDirector
                    .addProblemFact(new TestdataValue("added value"), solution.getValueList()::add));

            solutionWithProblemChangeReceived.await();
            assertThat(bestSolution.get().getValueList()).hasSize(valueCount + 1);
        } finally {
            solver.terminateEarly();
            executorService.shutdown();
        }
    }
}
