package ai.timefold.solver.core.api.solver;

import static ai.timefold.solver.core.api.solver.SolverStatus.NOT_SOLVING;
import static ai.timefold.solver.core.api.solver.SolverStatus.SOLVING_ACTIVE;
import static ai.timefold.solver.core.api.solver.SolverStatus.SOLVING_SCHEDULED;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertSolutionInitialized;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.solver.SolverJobBuilder.FirstInitializedSolutionConsumer;
import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.solver.DefaultSolverJob;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataConstraintProvider;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListConstraintProvider;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;
import org.assertj.core.api.Assertions;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class SolverManagerTest {

    private static final Function<Long, TestdataSolution> DEFAULT_PROBLEM_FINDER =
            problemId -> PlannerTestUtils.generateTestdataSolution("Generated solution " + problemId);
    private static final SolverManagerConfig SOLVER_MANAGER_CONFIG_WITH_1_PARALLEL_SOLVER =
            new SolverManagerConfig().withParallelSolverCount("1");

    @Test
    void create() {
        assertThatCode(() -> {
            var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
            SolverManager.create(solverConfig).close();
            var solverManagerConfig = new SolverManagerConfig();
            SolverManager.create(solverConfig, solverManagerConfig).close();
            SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
            SolverManager.create(solverFactory).close();
            SolverManager.create(solverFactory, solverManagerConfig).close();
        }).doesNotThrowAnyException();
    }

    @Test
    @Timeout(60)
    void solveBatch_2InParallel() throws ExecutionException, InterruptedException {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(createPhaseWithConcurrentSolvingStart(2), new ConstructionHeuristicPhaseConfig());
        try (var solverManager = createSolverManager(solverConfig, new SolverManagerConfig().withParallelSolverCount("2"))) {
            var solverJob1 = solverManager.solve(1L, PlannerTestUtils.generateTestdataSolution("s1"));
            var solverJob2 = solverManager.solve(2L, PlannerTestUtils.generateTestdataSolution("s2"));
            assertSolutionInitialized(solverJob1.getFinalBestSolution());
            assertSolutionInitialized(solverJob2.getFinalBestSolution());
        }
    }

    private static SolverManager<TestdataSolution, Long> createDefaultSolverManager(SolverConfig solverConfig) {
        return SolverManager.create(solverConfig);
    }

    private static SolverManager<TestdataSolution, Long> createSolverManagerWithOneSolver(SolverConfig solverConfig) {
        return createSolverManager(solverConfig, SOLVER_MANAGER_CONFIG_WITH_1_PARALLEL_SOLVER);
    }

    private static SolverManager<TestdataSolution, Long> createSolverManager(SolverConfig solverConfig,
            SolverManagerConfig solverManagerConfig) {
        return SolverManager.create(solverConfig, solverManagerConfig);
    }

    @SuppressWarnings("unchecked")
    private CustomPhaseConfig createPhaseWithConcurrentSolvingStart(int barrierPartiesCount) {
        var barrier = new CyclicBarrier(barrierPartiesCount);
        return new CustomPhaseConfig()
                .withCustomPhaseCommands(
                        (PhaseCommand<TestdataSolution>) (scoreDirector, isPhaseTerminated) -> {
                            try {
                                barrier.await();
                            } catch (InterruptedException | BrokenBarrierException e) {
                                fail("Cyclic barrier failed.");
                            }
                        });
    }

    @Test
    @Timeout(60)
    void getSolverStatus() throws InterruptedException, BrokenBarrierException, ExecutionException {
        var solverThreadReadyBarrier = new CyclicBarrier(2);
        var mainThreadReadyBarrier = new CyclicBarrier(2);
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new CustomPhaseConfig().withCustomPhaseCommands(
                        (scoreDirector, isPhaseTerminated) -> {
                            try {
                                solverThreadReadyBarrier.await();
                            } catch (InterruptedException | BrokenBarrierException e) {
                                fail("Cyclic barrier failed.");
                            }
                            try {
                                mainThreadReadyBarrier.await();
                            } catch (InterruptedException | BrokenBarrierException e) {
                                fail("Cyclic barrier failed.");
                            }
                        }), new ConstructionHeuristicPhaseConfig());
        // Only 1 solver can run at the same time to predict the solver status of each job.
        try (var solverManager = createSolverManagerWithOneSolver(solverConfig)) {
            var solverJob1 = solverManager.solve(1L, PlannerTestUtils.generateTestdataSolution("s1"));
            solverThreadReadyBarrier.await();
            var solverJob2 = solverManager.solve(2L, PlannerTestUtils.generateTestdataSolution("s2"));
            assertThat(solverManager.getSolverStatus(1L)).isEqualTo(SOLVING_ACTIVE);
            assertThat(solverJob1.getSolverStatus()).isEqualTo(SOLVING_ACTIVE);
            assertThat(solverManager.getSolverStatus(2L)).isEqualTo(SOLVING_SCHEDULED);
            assertThat(solverJob2.getSolverStatus()).isEqualTo(SOLVING_SCHEDULED);
            mainThreadReadyBarrier.await();
            solverThreadReadyBarrier.await();
            assertThat(solverManager.getSolverStatus(1L)).isEqualTo(NOT_SOLVING);
            assertThat(solverJob1.getSolverStatus()).isEqualTo(NOT_SOLVING);
            assertThat(solverManager.getSolverStatus(2L)).isEqualTo(SOLVING_ACTIVE);
            assertThat(solverJob2.getSolverStatus()).isEqualTo(SOLVING_ACTIVE);
            mainThreadReadyBarrier.await();
            solverJob1.getFinalBestSolution();
            solverJob2.getFinalBestSolution();
            assertThat(solverManager.getSolverStatus(1L)).isEqualTo(NOT_SOLVING);
            assertThat(solverJob1.getSolverStatus()).isEqualTo(NOT_SOLVING);
            assertThat(solverManager.getSolverStatus(2L)).isEqualTo(NOT_SOLVING);
            assertThat(solverJob2.getSolverStatus()).isEqualTo(NOT_SOLVING);
        }
    }

    @Test
    @Timeout(60)
    void exceptionInSolver() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new CustomPhaseConfig().withCustomPhaseCommands(
                        (scoreDirector, isPhaseTerminated) -> {
                            throw new IllegalStateException("exceptionInSolver");
                        }));
        try (var solverManager = createSolverManagerWithOneSolver(solverConfig)) {
            var exceptionCount = new AtomicInteger();
            var solverJob = solverManager.solveBuilder()
                    .withProblemId(1L)
                    .withProblemFinder(DEFAULT_PROBLEM_FINDER)
                    .withExceptionHandler((problemId, throwable) -> exceptionCount.incrementAndGet())
                    .run();
            assertThatThrownBy(solverJob::getFinalBestSolution)
                    .isInstanceOf(ExecutionException.class)
                    .hasRootCauseMessage("exceptionInSolver");
            assertThat(exceptionCount.get()).isEqualTo(1);
            assertThat(solverManager.getSolverStatus(1L)).isEqualTo(NOT_SOLVING);
            assertThat(solverJob.getSolverStatus()).isEqualTo(NOT_SOLVING);
        }
    }

    @Test
    @Timeout(60)
    void errorThrowableInSolver() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new CustomPhaseConfig().withCustomPhaseCommands(
                        (scoreDirector, isPhaseTerminated) -> {
                            throw new OutOfMemoryError("exceptionInSolver");
                        }));
        try (var solverManager = createSolverManagerWithOneSolver(solverConfig)) {
            var exceptionCount = new AtomicInteger();
            var solverJob = solverManager.solveBuilder()
                    .withProblemId(1L)
                    .withProblemFinder(DEFAULT_PROBLEM_FINDER)
                    .withExceptionHandler((problemId, throwable) -> exceptionCount.incrementAndGet())
                    .run();
            assertThatThrownBy(solverJob::getFinalBestSolution)
                    .isInstanceOf(ExecutionException.class)
                    .hasRootCauseMessage("exceptionInSolver");
            assertThat(exceptionCount.get()).isEqualTo(1);
            assertThat(solverManager.getSolverStatus(1L)).isEqualTo(NOT_SOLVING);
            assertThat(solverJob.getSolverStatus()).isEqualTo(NOT_SOLVING);
        }
    }

    @Test
    @Timeout(60)
    void exceptionInConsumer() throws InterruptedException {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());
        try (var solverManager = createSolverManagerWithOneSolver(solverConfig)) {
            var consumerInvoked = new CountDownLatch(1);
            var errorInConsumer = new AtomicReference<Throwable>();
            var solverJob1 = solverManager.solveBuilder()
                    .withProblemId(1L)
                    .withProblemFinder(DEFAULT_PROBLEM_FINDER)
                    .withFinalBestSolutionConsumer(bestSolution -> {
                        throw new IllegalStateException("exceptionInConsumer");
                    })
                    .withExceptionHandler((problemId, throwable) -> {
                        errorInConsumer.set(throwable);
                        consumerInvoked.countDown();
                    })
                    .run();

            consumerInvoked.await();
            assertThat(errorInConsumer.get())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("exceptionInConsumer");
            // Accessing the job's final best solution is necessary to guarantee that the solver is no longer solving.
            Assertions.assertThatCode(solverJob1::getFinalBestSolution).doesNotThrowAnyException();
            // Otherwise, the following assertion could fail.
            assertThat(solverManager.getSolverStatus(1L)).isEqualTo(NOT_SOLVING);
            assertThat(solverJob1.getSolverStatus()).isEqualTo(NOT_SOLVING);
        }
    }

    @Test
    @Timeout(60)
    void solveGenerics() throws ExecutionException, InterruptedException {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        try (var solverManager = createDefaultSolverManager(solverConfig)) {
            BiConsumer<Object, Object> exceptionHandler = (o1, o2) -> fail("Solving failed.");
            Consumer<Object> finalBestSolutionConsumer = o -> {
            };
            var solverJob = solverManager.solveBuilder()
                    .withProblemId(1L)
                    .withProblemFinder(DEFAULT_PROBLEM_FINDER)
                    .withFinalBestSolutionConsumer(finalBestSolutionConsumer)
                    .withExceptionHandler(exceptionHandler)
                    .run();
            solverJob.getFinalBestSolution();
        }
    }

    @Test
    @Timeout(60)
    void firstInitializedSolutionConsumerWithDefaultPhases() throws ExecutionException, InterruptedException {
        var hasInitializedSolution = new MutableBoolean();
        FirstInitializedSolutionConsumer<Object> initializedSolutionConsumer =
                (ignore, isTerminatedEarly) -> hasInitializedSolution.setValue(!isTerminatedEarly);

        // Default configuration
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withTerminationConfig(new TerminationConfig()
                        .withUnimprovedMillisecondsSpentLimit(1L));
        try (var solverManager = createDefaultSolverManager(solverConfig)) {
            var solverJob = solverManager.solveBuilder()
                    .withProblemId(1L)
                    .withProblemFinder(DEFAULT_PROBLEM_FINDER)
                    .withFirstInitializedSolutionConsumer(initializedSolutionConsumer)
                    .run();
            solverJob.getFinalBestSolution();
            assertThat(hasInitializedSolution.booleanValue()).isTrue();
        }
    }

    @Test
    @Timeout(60)
    void firstInitializedSolutionConsumerWithSingleCHPhase() throws ExecutionException, InterruptedException {
        var hasInitializedSolution = new MutableBoolean();
        FirstInitializedSolutionConsumer<Object> initializedSolutionConsumer =
                (ignore, isTerminatedEarly) -> hasInitializedSolution.setValue(!isTerminatedEarly);

        // Only CH
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());
        try (var solverManager = createDefaultSolverManager(solverConfig)) {
            var solverJob = solverManager.solveBuilder()
                    .withProblemId(1L)
                    .withProblemFinder(DEFAULT_PROBLEM_FINDER)
                    .withFirstInitializedSolutionConsumer(initializedSolutionConsumer)
                    .run();
            solverJob.getFinalBestSolution();
            assertThat(hasInitializedSolution.booleanValue()).isFalse();
            hasInitializedSolution.setFalse();
        }
    }

    @Test
    @Timeout(60)
    void firstInitializedSolutionConsumerWithSingleLSPhase() throws ExecutionException, InterruptedException {
        var hasInitializedSolution = new MutableBoolean();
        FirstInitializedSolutionConsumer<Object> initializedSolutionConsumer =
                (ignore, isTerminatedEarly) -> hasInitializedSolution.setValue(!isTerminatedEarly);
        // Only LS
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new LocalSearchPhaseConfig())
                .withTerminationConfig(new TerminationConfig()
                        .withBestScoreLimit("0"));
        try (var solverManager = createDefaultSolverManager(solverConfig)) {
            var initializedSolution = PlannerTestUtils.generateTestdataSolution("s1");
            initializedSolution.getEntityList().forEach(e -> e.setValue(initializedSolution.getValueList().get(0)));

            var solverJob = solverManager.solveBuilder()
                    .withProblemId(1L)
                    .withProblemFinder(o -> initializedSolution)
                    .withFirstInitializedSolutionConsumer(initializedSolutionConsumer)
                    .withFinalBestSolutionConsumer(ignore -> {
                    })
                    .run();
            solverJob.getFinalBestSolution();
            assertThat(hasInitializedSolution.booleanValue()).isFalse();
        }
    }

    @Test
    @Timeout(60)
    void firstInitializedSolutionConsumerEarlyTerminatedCH() throws InterruptedException {
        var consumerCalled = new AtomicBoolean();
        var hasInitializedSolution = new AtomicBoolean();
        FirstInitializedSolutionConsumer<Object> initializedSolutionConsumer = (ignore, isTerminatedEarly) -> {
            consumerCalled.set(true);
            hasInitializedSolution.set(!isTerminatedEarly);
        };

        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withConstraintProviderClass(TestdataConstraintProvider.class)
                .withPhases(new ConstructionHeuristicPhaseConfig()
                        .withTerminationConfig(new TerminationConfig().withStepCountLimit(1)),
                        new LocalSearchPhaseConfig());
        try (var solverManager = createDefaultSolverManager(solverConfig)) {
            // The solution will produce a CH that takes 2 steps.
            // The CH is configured to terminate after 1 step, guaranteeing uninitialized solution.
            Function<Object, TestdataSolution> problemFinder = o -> {
                var solution = TestdataSolution.generateSolution(2, 2);
                // Uninitialize the solution.
                solution.getEntityList().forEach(e -> e.setValue(null));
                return solution;
            };

            var solverJob = solverManager.solveBuilder()
                    .withProblemId(1L)
                    .withProblemFinder(problemFinder)
                    .withFirstInitializedSolutionConsumer(initializedSolutionConsumer)
                    .run();
            try {
                solverJob.getFinalBestSolution();
            } catch (ExecutionException e) {
                // The LS will attempt to start after the CH.
                // It will fail, because LS expects an initialized solution.
                // Ignore the failure, because there is no other way to test this,
                // other than to use a time-based termination for the CH,
                // which would make the test non-deterministic.
                assertThat(e).rootCause()
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("needs to start from an initialized solution");
            } finally {
                assertThat(consumerCalled).isTrue();
                assertThat(hasInitializedSolution).isFalse();
            }
        }
    }

    @Test
    @Timeout(60)
    void firstInitializedSolutionConsumerEarlyTerminatedCHListVar() throws InterruptedException, ExecutionException {
        var consumerCalled = new AtomicBoolean();
        var hasInitializedSolution = new AtomicBoolean();
        FirstInitializedSolutionConsumer<Object> initializedSolutionConsumer = (ignore, isTerminatedEarly) -> {
            consumerCalled.set(true);
            hasInitializedSolution.set(!isTerminatedEarly);
        };

        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataAllowsUnassignedValuesListSolution.class)
                .withEntityClasses(TestdataAllowsUnassignedValuesListEntity.class,
                        TestdataAllowsUnassignedValuesListValue.class)
                .withConstraintProviderClass(TestdataAllowsUnassignedValuesListConstraintProvider.class)
                .withPhases(new ConstructionHeuristicPhaseConfig()
                        .withTerminationConfig(new TerminationConfig().withStepCountLimit(1)),
                        new LocalSearchPhaseConfig()
                                .withTerminationConfig(new TerminationConfig().withStepCountLimit(0)));
        try (var solverManager = SolverManager.<TestdataAllowsUnassignedValuesListSolution, Long> create(solverConfig)) {
            // The solution will produce a CH that takes 2 steps.
            // The CH is configured to terminate after 1 step, guaranteeing early termination.
            Function<Object, TestdataAllowsUnassignedValuesListSolution> problemFinder = o -> {
                var solution = new TestdataAllowsUnassignedValuesListSolution();
                solution.setEntityList(IntStream.range(0, 2)
                        .mapToObj(i -> new TestdataAllowsUnassignedValuesListEntity("Generated Entity " + i))
                        .toList());
                solution.setValueList(IntStream.range(0, 2)
                        .mapToObj(i -> new TestdataAllowsUnassignedValuesListValue("Generated Value " + i))
                        .toList());
                return solution;
            };

            var solverJob = solverManager.solveBuilder()
                    .withProblemId(1L)
                    .withProblemFinder(problemFinder)
                    .withFirstInitializedSolutionConsumer(initializedSolutionConsumer)
                    .run();
            solverJob.getFinalBestSolution(); // LS will start, but terminate immediately.
            assertThat(consumerCalled).isTrue();
            assertThat(hasInitializedSolution).isFalse();
        }
    }

    @Test
    @Timeout(60)
    void firstInitializedSolutionConsumerWith2CHAndLS() throws ExecutionException, InterruptedException {
        var hasInitializedSolution = new MutableBoolean();
        FirstInitializedSolutionConsumer<Object> initializedSolutionConsumer =
                (ignore, isTerminatedEarly) -> hasInitializedSolution.setValue(!isTerminatedEarly);

        // CH - CH - LS
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig(), new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig())
                .withTerminationConfig(new TerminationConfig()
                        .withUnimprovedMillisecondsSpentLimit(1L));
        hasInitializedSolution.setFalse();
        try (var solverManager = createDefaultSolverManager(solverConfig)) {
            var solverJob = solverManager.solveBuilder()
                    .withProblemId(1L)
                    .withProblemFinder(DEFAULT_PROBLEM_FINDER)
                    .withFirstInitializedSolutionConsumer(initializedSolutionConsumer)
                    .run();
            solverJob.getFinalBestSolution();
            assertThat(hasInitializedSolution.booleanValue()).isTrue();
        }
    }

    @Test
    @Timeout(60)
    void firstInitializedSolutionConsumerWithCustomAndCHAndLS() throws ExecutionException, InterruptedException {
        var hasInitializedSolution = new MutableBoolean();
        FirstInitializedSolutionConsumer<Object> initializedSolutionConsumer =
                (ignore, isTerminatedEarly) -> hasInitializedSolution.setValue(!isTerminatedEarly);

        // CS - CH - LS
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new CustomPhaseConfig()
                        .withCustomPhaseCommands((scoreDirector,
                                isPhaseTerminated) -> assertThat(hasInitializedSolution.booleanValue()).isFalse()),
                        new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig())
                .withTerminationConfig(new TerminationConfig()
                        .withUnimprovedMillisecondsSpentLimit(1L));
        try (var solverManager = createDefaultSolverManager(solverConfig)) {
            var solverJob = solverManager.solveBuilder()
                    .withProblemId(1L)
                    .withProblemFinder(DEFAULT_PROBLEM_FINDER)
                    .withFirstInitializedSolutionConsumer(initializedSolutionConsumer)
                    .run();
            solverJob.getFinalBestSolution();
            assertThat(hasInitializedSolution.booleanValue()).isTrue();
        }
    }

    @Test
    @Timeout(60)
    void firstInitializedSolutionConsumerWithCHAndCustomAndLS() throws ExecutionException, InterruptedException {
        var hasInitializedSolution = new MutableBoolean();
        FirstInitializedSolutionConsumer<Object> initializedSolutionConsumer =
                (ignore, isTerminatedEarly) -> hasInitializedSolution.setValue(!isTerminatedEarly);

        // CH - CS - LS
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(
                        new ConstructionHeuristicPhaseConfig(),
                        new CustomPhaseConfig()
                                .withCustomPhaseCommands(
                                        (scoreDirector, isPhaseTerminated) -> {
                                            assertThat(hasInitializedSolution.booleanValue()).isFalse();
                                        }),
                        new LocalSearchPhaseConfig())
                .withTerminationConfig(new TerminationConfig()
                        .withUnimprovedMillisecondsSpentLimit(1L));
        try (var solverManager = createDefaultSolverManager(solverConfig)) {
            var solverJob = solverManager.solveBuilder()
                    .withProblemId(1L)
                    .withProblemFinder(DEFAULT_PROBLEM_FINDER)
                    .withFirstInitializedSolutionConsumer(initializedSolutionConsumer)
                    .run();
            solverJob.getFinalBestSolution();
            assertThat(hasInitializedSolution.booleanValue()).isTrue();
        }
    }

    @Test
    @Timeout(60)
    void firstInitializedSolutionConsumerWith2Custom() throws ExecutionException, InterruptedException {
        var hasInitializedSolution = new MutableBoolean();
        FirstInitializedSolutionConsumer<Object> initializedSolutionConsumer =
                (ignore, isTerminatedEarly) -> hasInitializedSolution.setValue(!isTerminatedEarly);

        // CS (CH) - CS (LS)
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(
                        new CustomPhaseConfig().withCustomPhaseCommands((scoreDirector,
                                isPhaseTerminated) -> assertThat(hasInitializedSolution.booleanValue()).isFalse()),
                        new CustomPhaseConfig().withCustomPhaseCommands((scoreDirector,
                                isPhaseTerminated) -> assertThat(hasInitializedSolution.booleanValue()).isFalse()))
                .withTerminationConfig(new TerminationConfig()
                        .withUnimprovedMillisecondsSpentLimit(1L));
        try (var solverManager = createDefaultSolverManager(solverConfig)) {
            var solverJob = solverManager.solveBuilder()
                    .withProblemId(1L)
                    .withProblemFinder(DEFAULT_PROBLEM_FINDER)
                    .withFirstInitializedSolutionConsumer(initializedSolutionConsumer)
                    .run();
            solverJob.getFinalBestSolution();
            assertThat(hasInitializedSolution.booleanValue()).isFalse();
        }
    }

    @Test
    @Timeout(60)
    void testStartJobConsumer() throws ExecutionException, InterruptedException {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        try (var solverManager = createDefaultSolverManager(solverConfig)) {
            var started = new MutableInt(0);
            var solverJob = solverManager.solveBuilder()
                    .withProblemId(1L)
                    .withProblemFinder(DEFAULT_PROBLEM_FINDER)
                    .withSolverJobStartedConsumer(solution -> started.increment())
                    .run();
            solverJob.getFinalBestSolution();
            assertThat(started.getValue()).isOne();
        }
    }

    @Test
    void solveWithOverride() {
        // Default spent limit is 1L
        var terminationConfig = new TerminationConfig()
                .withSpentLimit(Duration.ofSeconds(1L));
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withTerminationConfig(terminationConfig);
        solverConfig.withTerminationConfig(terminationConfig);

        try (var solverManager = createDefaultSolverManager(solverConfig)) {
            var problem = PlannerTestUtils.generateTestdataSolution("s1");

            SolverScope<TestdataSolution> solverScope = mock(SolverScope.class);
            doReturn(50L).when(solverScope).calculateTimeMillisSpentUpToNow();

            var solverJob = (DefaultSolverJob<TestdataSolution, Long>) solverManager.solve(1L, problem);
            assertThat(solverJob.getSolverTermination().calculateSolverTimeGradient(solverScope)).isEqualTo(0.05);

            // Spent limit overridden by 100L
            var configOverride = new SolverConfigOverride<TestdataSolution>()
                    .withTerminationConfig(new TerminationConfig().withSpentLimit(Duration.ofMillis(100L)));
            solverJob = (DefaultSolverJob<TestdataSolution, Long>) solverManager.solveBuilder()
                    .withProblemId(2L)
                    .withProblem(problem)
                    .withConfigOverride(configOverride)
                    .run();
            assertThat(solverJob.getSolverTermination().calculateSolverTimeGradient(solverScope)).isEqualTo(0.5);
        }
    }

    @Test
    void testScoreCalculationCountForFinishedJob() throws ExecutionException, InterruptedException {
        // Terminate after exactly 5 score calculations
        var terminationConfig = new TerminationConfig()
                .withScoreCalculationCountLimit(5L);
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withEnvironmentMode(EnvironmentMode.NO_ASSERT)
                .withTerminationConfig(terminationConfig);

        try (var solverManager = createDefaultSolverManager(solverConfig)) {
            var problem = PlannerTestUtils.generateTestdataSolution("s1");
            var solverJob = solverManager.solveBuilder()
                    .withProblemId(2L)
                    .withProblem(problem)
                    .run();

            solverJob.getFinalBestSolution();
            // The score is calculated during the solving starting phase without applying any moves.
            // This explains why the count has one more unit.
            assertThat(solverJob.getScoreCalculationCount()).isEqualTo(5L);
            assertThat(solverJob.getMoveEvaluationCount()).isEqualTo(4L);

            // Score calculation speed and solve duration are non-deterministic.
            // On an exceptionally fast machine, getSolvingDuration() can return Duration.ZERO.
            // On an exceptionally slow machine, getScoreCalculationSpeed() can be 0 due to flooring
            // (i.e. by taking more than 5 seconds to finish solving).
            assertThat(solverJob.getSolvingDuration()).isGreaterThanOrEqualTo(Duration.ZERO);
            assertThat(solverJob.getScoreCalculationSpeed()).isNotNegative();
            assertThat(solverJob.getMoveEvaluationSpeed()).isNotNegative();
        }
    }

    @Test
    void testProblemSizeStatisticsForFinishedJob() throws ExecutionException, InterruptedException {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);

        try (var solverManager = createDefaultSolverManager(solverConfig)) {
            var problem = PlannerTestUtils.generateTestdataSolution("s1", 2);
            var solverJob = solverManager.solveBuilder()
                    .withProblemId(2L)
                    .withProblem(problem)
                    .run();

            solverJob.getFinalBestSolution();
            var problemSizeStatistics = solverJob.getProblemSizeStatistics();
            assertThat(problemSizeStatistics.entityCount()).isEqualTo(2L);
            assertThat(problemSizeStatistics.variableCount()).isEqualTo(2L);
            assertThat(problemSizeStatistics.approximateValueCount()).isEqualTo(2L);
            assertThat(problemSizeStatistics.approximateProblemScaleAsFormattedString()).isEqualTo("4");
        }
    }

    @Test
    @Timeout(60)
    void testProblemSizeStatisticsForWaitingJob() throws InterruptedException, ExecutionException {
        var solvingPausedLatch = new CountDownLatch(1);
        var pausedPhaseConfig = new CustomPhaseConfig().withCustomPhaseCommands(
                (scoreDirector, isPhaseTerminated) -> {
                    try {
                        solvingPausedLatch.await();
                    } catch (InterruptedException e) {
                        fail("CountDownLatch failed.");
                    }
                });

        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(pausedPhaseConfig, new ConstructionHeuristicPhaseConfig());
        // Allow only a single active solver.
        try (var solverManager = createSolverManagerWithOneSolver(solverConfig)) {
            // The first solver waits until the test sends a problem change.
            solverManager.solve(1L, PlannerTestUtils.generateTestdataSolution("s1", 4));

            // The second solver is scheduled and waits for the fist solver to finish.
            var secondProblemId = 2L;
            var entityAndValueCount = 4;
            var bestSolution = new AtomicReference<TestdataSolution>();
            var waitingSolverJob = solverManager.solveBuilder()
                    .withProblemId(secondProblemId)
                    .withProblemFinder(id -> PlannerTestUtils.generateTestdataSolution("s2", entityAndValueCount))
                    .withBestSolutionConsumer(bestSolution::set)
                    .run();

            var problemSizeStatistics = waitingSolverJob.getProblemSizeStatistics();
            assertThat(problemSizeStatistics.entityCount()).isEqualTo(4L);
            assertThat(problemSizeStatistics.variableCount()).isEqualTo(4L);
            assertThat(problemSizeStatistics.approximateValueCount()).isEqualTo(4L);
            assertThat(problemSizeStatistics.approximateProblemScaleAsFormattedString()).isEqualTo("256");

            var futureChange = solverManager
                    .addProblemChange(secondProblemId, (workingSolution, problemChangeDirector) -> {
                        problemChangeDirector.addProblemFact(new TestdataValue("addedValue"),
                                workingSolution.getValueList()::add);
                    });

            // The first solver can proceed. When it finishes, the second solver starts solving and picks up the change.
            solvingPausedLatch.countDown();
            futureChange.get();
            assertThat(futureChange).isCompleted();
            assertThat(bestSolution.get().getValueList()).hasSize(entityAndValueCount + 1);
            problemSizeStatistics = waitingSolverJob.getProblemSizeStatistics();
            assertThat(problemSizeStatistics.entityCount()).isEqualTo(4L);
            assertThat(problemSizeStatistics.variableCount()).isEqualTo(4L);
            assertThat(problemSizeStatistics.approximateValueCount()).isEqualTo(5L);
            assertThat(problemSizeStatistics.approximateProblemScaleAsFormattedString()).isEqualTo("625");
        }
    }

    @Test
    void testSolveBuilderForExistingSolvingMethods() {
        SolverJobBuilder<TestdataSolution, Long> solverJobBuilder = mock(SolverJobBuilder.class);
        SolverManager<TestdataSolution, Long> solverManager = mock(SolverManager.class);

        doReturn(solverJobBuilder).when(solverManager).solveBuilder();
        doReturn(solverJobBuilder).when(solverJobBuilder).withProblemId(anyLong());
        doReturn(solverJobBuilder).when(solverJobBuilder).withProblem(any());
        doReturn(solverJobBuilder).when(solverJobBuilder).withFinalBestSolutionConsumer(any());
        doReturn(solverJobBuilder).when(solverJobBuilder).withExceptionHandler(any());
        doReturn(solverJobBuilder).when(solverJobBuilder).withProblemFinder(any());
        doReturn(solverJobBuilder).when(solverJobBuilder).withBestSolutionConsumer(any());

        doCallRealMethod().when(solverManager).solve(any(Long.class), any(TestdataSolution.class));
        solverManager.solve(1L, mock(TestdataSolution.class));
        verify(solverJobBuilder, times(1)).withProblemId(anyLong());
        verify(solverJobBuilder, times(1)).withProblem(any());

        doCallRealMethod().when(solverManager).solve(any(Long.class), any(TestdataSolution.class), any(Consumer.class));
        solverManager.solve(1L, mock(TestdataSolution.class), mock(Consumer.class));
        verify(solverJobBuilder, times(2)).withProblemId(anyLong());
        verify(solverJobBuilder, times(2)).withProblem(any());
        verify(solverJobBuilder, times(1)).withFinalBestSolutionConsumer(any());

        doCallRealMethod().when(solverManager).solveAndListen(any(Long.class), any(TestdataSolution.class),
                any(Consumer.class));
        solverManager.solveAndListen(1L, mock(TestdataSolution.class), mock(Consumer.class));
        verify(solverJobBuilder, times(3)).withProblemId(anyLong());
        verify(solverJobBuilder, times(3)).withProblem(any());
        verify(solverJobBuilder, times(1)).withBestSolutionConsumer(any());
    }

    @Test
    @Timeout(60)
    void solveWithBuilder() throws InterruptedException, BrokenBarrierException {
        var startedBarrier = new CyclicBarrier(2);
        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        try (var solverManager = createDefaultSolverManager(solverConfig)) {
            BiConsumer<Object, Object> exceptionHandler = (o1, o2) -> fail("Solving failed.");
            var finalBestSolution = new MutableObject<TestdataSolution>();
            Consumer<TestdataSolution> finalBestConsumer = o -> {
                finalBestSolution.setValue(o);
                try {
                    startedBarrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            };
            solverManager.solveBuilder()
                    .withProblemId(1L)
                    .withProblemFinder(DEFAULT_PROBLEM_FINDER)
                    .withFinalBestSolutionConsumer(finalBestConsumer)
                    .withExceptionHandler(exceptionHandler)
                    .run();

            startedBarrier.await();
            assertThat(finalBestSolution.getValue()).isNotNull();
        }
    }

    @Test
    @Timeout(60)
    void solveAndListenWithBuilder() throws InterruptedException, BrokenBarrierException {
        var startedBarrier = new CyclicBarrier(2);
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        try (var solverManager = createDefaultSolverManager(solverConfig)) {
            BiConsumer<Object, Object> exceptionHandler = (o1, o2) -> fail("Solving failed.");
            var finalBestSolution = new MutableObject<TestdataSolution>();
            Consumer<TestdataSolution> finalBestConsumer = o -> {
                finalBestSolution.setValue(o);
                try {
                    startedBarrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            };
            var bestSolution = new MutableObject<TestdataSolution>();
            Consumer<TestdataSolution> bestConsumer = bestSolution::setValue;
            solverManager.solveBuilder()
                    .withProblemId(1L)
                    .withProblemFinder(DEFAULT_PROBLEM_FINDER)
                    .withFinalBestSolutionConsumer(finalBestConsumer)
                    .withBestSolutionConsumer(bestConsumer)
                    .withExceptionHandler(exceptionHandler)
                    .run();

            startedBarrier.await();
            assertThat(finalBestSolution.getValue()).isNotNull();
            assertThat(bestSolution.getValue()).isNotNull();
        }
    }

    @Test
    @Timeout(60)
    void skipAhead() throws ExecutionException, InterruptedException {
        var latch = new CountDownLatch(1);
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new CustomPhaseConfig().withCustomPhaseCommands(
                        (ScoreDirector<TestdataSolution> scoreDirector, BooleanSupplier booleanSupplier) -> {
                            var solution = scoreDirector.getWorkingSolution();
                            var entity = solution.getEntityList().get(0);
                            scoreDirector.beforeVariableChanged(entity, "value");
                            entity.setValue(solution.getValueList().get(0));
                            scoreDirector.afterVariableChanged(entity, "value");
                            scoreDirector.triggerVariableListeners();
                        }, (ScoreDirector<TestdataSolution> scoreDirector, BooleanSupplier booleanSupplier) -> {
                            var solution = scoreDirector.getWorkingSolution();
                            var entity = solution.getEntityList().get(1);
                            scoreDirector.beforeVariableChanged(entity, "value");
                            entity.setValue(solution.getValueList().get(1));
                            scoreDirector.afterVariableChanged(entity, "value");
                            scoreDirector.triggerVariableListeners();
                        }, (ScoreDirector<TestdataSolution> scoreDirector, BooleanSupplier booleanSupplier) -> {
                            var solution = scoreDirector.getWorkingSolution();
                            var entity = solution.getEntityList().get(2);
                            scoreDirector.beforeVariableChanged(entity, "value");
                            entity.setValue(solution.getValueList().get(2));
                            scoreDirector.afterVariableChanged(entity, "value");
                            scoreDirector.triggerVariableListeners();
                        }, (ScoreDirector<TestdataSolution> scoreDirector, BooleanSupplier booleanSupplier) -> {
                            // In the next best solution event, both e1 and e2 are definitely not null (but e3 might be).
                            latch.countDown();
                            var solution = scoreDirector.getWorkingSolution();
                            var entity = solution.getEntityList().get(3);
                            scoreDirector.beforeVariableChanged(entity, "value");
                            entity.setValue(solution.getValueList().get(3));
                            scoreDirector.afterVariableChanged(entity, "value");
                            scoreDirector.triggerVariableListeners();
                        }));
        try (var solverManager = createSolverManagerWithOneSolver(solverConfig)) {
            var bestSolutionCount = new AtomicInteger();
            var finalBestSolutionCount = new AtomicInteger();
            var consumptionError = new AtomicReference<Throwable>();
            var finalBestSolutionConsumed = new CountDownLatch(1);
            var solverJob = solverManager.solveBuilder()
                    .withProblemId(1L)
                    .withProblemFinder(problemId -> PlannerTestUtils.generateTestdataSolution("s1", 4))
                    .withBestSolutionConsumer(bestSolution -> {
                        var isFirstReceivedSolution = bestSolutionCount.incrementAndGet() == 1;
                        if (bestSolution.getEntityList().get(1).getValue() == null) {
                            // This best solution may be skipped as well.
                            try {
                                latch.await();
                            } catch (InterruptedException e) {
                                fail("Latch failed.");
                            }
                        } else if (bestSolution.getEntityList().get(2).getValue() == null && !isFirstReceivedSolution) {
                            fail("No skip ahead occurred: both e2 and e3 are null in a best solution event.");
                        }
                    })
                    .withFinalBestSolutionConsumer(finalBestSolution -> {
                        finalBestSolutionCount.incrementAndGet();
                        finalBestSolutionConsumed.countDown();
                    })
                    .withExceptionHandler((problemId, throwable) -> consumptionError.set(throwable))
                    .run();
            assertSolutionInitialized(solverJob.getFinalBestSolution());
            // EventCount can be 2 or 3, depending on the race, but it can never be 4.
            assertThat(bestSolutionCount).hasValueLessThan(4);
            finalBestSolutionConsumed.await();
            assertThat(finalBestSolutionCount.get()).isEqualTo(1);
            if (consumptionError.get() != null) {
                fail("Error in the best solution consumer.", consumptionError.get());
            }
        }
    }

    @Test
    @Timeout(600)
    void terminateEarly() throws InterruptedException, BrokenBarrierException {
        var startedBarrier = new CyclicBarrier(2);
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withTerminationConfig(new TerminationConfig())
                .withPhases(new CustomPhaseConfig().withCustomPhaseCommands((scoreDirector, isPhaseTerminated) -> {
                    try {
                        startedBarrier.await();
                    } catch (InterruptedException | BrokenBarrierException e) {
                        throw new IllegalStateException("The startedBarrier failed.", e);
                    }
                }),
                        new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig());

        try (var solverManager = createSolverManagerWithOneSolver(solverConfig)) {
            var solverJob1 = solverManager.solve(1L,
                    PlannerTestUtils.generateTestdataSolution("s1", 4));
            var solverJob2 = solverManager.solve(2L,
                    PlannerTestUtils.generateTestdataSolution("s2", 4));
            var solverJob3 = solverManager.solve(3L,
                    PlannerTestUtils.generateTestdataSolution("s3", 4));

            // Give solver 1 enough time to start
            startedBarrier.await();
            assertThat(solverManager.getSolverStatus(1L)).isEqualTo(SOLVING_ACTIVE);
            assertThat(solverJob1.getSolverStatus()).isEqualTo(SOLVING_ACTIVE);
            assertThat(solverManager.getSolverStatus(2L)).isEqualTo(SOLVING_SCHEDULED);
            assertThat(solverJob2.getSolverStatus()).isEqualTo(SOLVING_SCHEDULED);
            assertThat(solverManager.getSolverStatus(3L)).isEqualTo(SOLVING_SCHEDULED);
            assertThat(solverJob3.getSolverStatus()).isEqualTo(SOLVING_SCHEDULED);

            // Terminate solver 2 before it begins
            solverManager.terminateEarly(2L);
            assertThat(solverManager.getSolverStatus(1L)).isEqualTo(SOLVING_ACTIVE);
            assertThat(solverJob1.getSolverStatus()).isEqualTo(SOLVING_ACTIVE);
            assertThat(solverManager.getSolverStatus(2L)).isEqualTo(NOT_SOLVING);
            assertThat(solverJob2.getSolverStatus()).isEqualTo(NOT_SOLVING);
            assertThat(solverManager.getSolverStatus(3L)).isEqualTo(SOLVING_SCHEDULED);
            assertThat(solverJob3.getSolverStatus()).isEqualTo(SOLVING_SCHEDULED);

            // Terminate solver 1 while it is running, allowing solver 3 to start
            solverManager.terminateEarly(1L);
            assertThat(solverManager.getSolverStatus(1L)).isEqualTo(NOT_SOLVING);
            assertThat(solverJob1.getSolverStatus()).isEqualTo(NOT_SOLVING);
            // Give solver 3 enough time to start
            startedBarrier.await();
            assertThat(solverManager.getSolverStatus(3L)).isEqualTo(SOLVING_ACTIVE);
            assertThat(solverJob3.getSolverStatus()).isEqualTo(SOLVING_ACTIVE);

            // Terminate solver 3 while it is running
            solverManager.terminateEarly(3L);
            assertThat(solverManager.getSolverStatus(3L)).isEqualTo(NOT_SOLVING);
            assertThat(solverJob3.getSolverStatus()).isEqualTo(NOT_SOLVING);
        }
    }

    private void assertInitializedJobs(List<SolverJob<TestdataSolution, Long>> jobs)
            throws InterruptedException, ExecutionException {
        for (var job : jobs) {
            // Method getFinalBestSolution() waits for the solving to finish, therefore it ensures synchronization.
            assertSolutionInitialized(job.getFinalBestSolution());
        }
    }

    @Test
    @Timeout(60)
    void submitMoreProblemsThanCpus_allGetSolved() throws InterruptedException, ExecutionException {
        // Use twice the amount of problems than available processors.
        var problemCount = Runtime.getRuntime().availableProcessors() * 2;
        try (var solverManager = createSolverManagerTestableByDifferentConsumers()) {
            assertSolveWithoutConsumer(problemCount, solverManager);
            assertSolveWithConsumer(problemCount, solverManager, true);
            assertSolveWithConsumer(problemCount, solverManager, false);
        }
    }

    private SolverManager<TestdataSolution, Long> createSolverManagerTestableByDifferentConsumers() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(IntStream.of(0, 1)
                        .mapToObj(x -> new CustomPhaseConfig().withCustomPhaseCommands(
                                (ScoreDirector<TestdataSolution> scoreDirector, BooleanSupplier booleanSupplier) -> {
                                    var solution = scoreDirector.getWorkingSolution();
                                    var entity = solution.getEntityList().get(x);
                                    scoreDirector.beforeVariableChanged(entity, "value");
                                    entity.setValue(solution.getValueList().get(x));
                                    scoreDirector.afterVariableChanged(entity, "value");
                                    scoreDirector.triggerVariableListeners();
                                }))
                        .toArray(PhaseConfig[]::new));
        return createDefaultSolverManager(solverConfig);
    }

    private void assertSolveWithoutConsumer(int problemCount, SolverManager<TestdataSolution, Long> solverManager)
            throws InterruptedException, ExecutionException {
        var jobs = new ArrayList<SolverJob<TestdataSolution, Long>>(problemCount);
        for (long id = 0; id < problemCount; id++) {
            jobs.add(solverManager.solve(id, PlannerTestUtils.generateTestdataSolution(String.format("s%d", id))));
        }
        assertInitializedJobs(jobs);
    }

    private void assertSolveWithConsumer(int problemCount, SolverManager<TestdataSolution, Long> solverManager,
            boolean listenWhileSolving)
            throws ExecutionException, InterruptedException {

        // Two solutions should be created for every problem.
        var solutionMap = new HashMap<Long, List<TestdataSolution>>(problemCount * 2);
        var finalBestSolutionConsumed = new CountDownLatch(problemCount);
        var jobs = new ArrayList<SolverJob<TestdataSolution, Long>>(problemCount);

        for (var id = 0L; id < problemCount; id++) {
            var consumedBestSolutions = Collections.synchronizedList(new ArrayList<TestdataSolution>());
            var solutionName = String.format("s%d", id);
            if (listenWhileSolving) {
                jobs.add(solverManager.solveBuilder()
                        .withProblemId(id)
                        .withProblemFinder(problemId -> PlannerTestUtils.generateTestdataSolution(solutionName, 2))
                        .withBestSolutionConsumer(consumedBestSolutions::add)
                        .withFinalBestSolutionConsumer(finalBestSolution -> finalBestSolutionConsumed.countDown())
                        .run());
            } else {
                jobs.add(solverManager.solveBuilder()
                        .withProblemId(id)
                        .withProblemFinder(problemId -> PlannerTestUtils.generateTestdataSolution(solutionName, 2))
                        .withFinalBestSolutionConsumer(finalBestSolution -> {
                            consumedBestSolutions.add(finalBestSolution);
                            finalBestSolutionConsumed.countDown();
                        })
                        .run());
            }
            solutionMap.put(id, consumedBestSolutions);
        }
        assertInitializedJobs(jobs);

        finalBestSolutionConsumed.await(); // Wait till all final best solutions have been consumed.
        if (listenWhileSolving) {
            assertConsumedSolutionsWithListeningWhileSolving(solutionMap);
        } else {
            assertConsumedSolutions(solutionMap);
        }
    }

    private void assertConsumedSolutions(Map<Long, List<TestdataSolution>> consumedSolutions) {
        for (var consumedSolution : consumedSolutions.values()) {
            assertThat(consumedSolution).hasSize(1);
            assertConsumedFinalBestSolution(consumedSolution.get(0));
        }
    }

    private void assertConsumedSolutionsWithListeningWhileSolving(Map<Long, List<TestdataSolution>> consumedSolutions) {
        consumedSolutions.forEach((problemId, bestSolutions) -> {
            if (bestSolutions.size() == 2) {
                assertConsumedFirstBestSolution(bestSolutions.get(0));
                assertConsumedFinalBestSolution(bestSolutions.get(1));
            } else if (bestSolutions.size() == 1) { // The fist best solution has been skipped.
                assertConsumedFinalBestSolution(bestSolutions.get(0));
            } else {
                fail("Unexpected number of received best solutions ("
                        + bestSolutions.size() + "). Should be either 1 or 2.");
            }
        });
    }

    private void assertConsumedFinalBestSolution(TestdataSolution solution) {
        var entity = solution.getEntityList().get(0);
        assertThat(entity.getCode()).isEqualTo("e1");
        assertThat(entity.getValue().getCode()).isEqualTo("v1");
        entity = solution.getEntityList().get(1);
        assertThat(entity.getCode()).isEqualTo("e2");
        assertThat(entity.getValue().getCode()).isEqualTo("v2");
    }

    private void assertConsumedFirstBestSolution(TestdataSolution solution) {
        var entity = solution.getEntityList().get(0);
        assertThat(entity.getCode()).isEqualTo("e1");
        assertThat(entity.getValue().getCode()).isEqualTo("v1");
        entity = solution.getEntityList().get(1);
        assertThat(entity.getCode()).isEqualTo("e2");
        assertThat(entity.getValue()).isNull();
    }

    @Test
    @Timeout(60)
    void runSameIdProcesses_throwsIllegalStateException() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(createPhaseWithConcurrentSolvingStart(2));

        try (var solverManager = createDefaultSolverManager(solverConfig)) {
            solverManager.solve(1L, PlannerTestUtils.generateTestdataSolution("s1"));
            assertThatThrownBy(() -> solverManager.solve(1L, PlannerTestUtils.generateTestdataSolution("s1")))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("already solving");
        }
    }

    @Test
    @Timeout(60)
    void addProblemChange() throws InterruptedException, ExecutionException {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        solverConfig.setDaemon(true);
        try (var solverManager = createDefaultSolverManager(solverConfig)) {
            var problemId = 1L;
            var entityAndValueCount = 4;
            var bestSolution = new AtomicReference<TestdataSolution>();
            solverManager.solveBuilder()
                    .withProblemId(problemId)
                    .withProblemFinder(id -> PlannerTestUtils.generateTestdataSolution("s1", entityAndValueCount))
                    .withBestSolutionConsumer(bestSolution::set)
                    .run();

            var futureChange = solverManager
                    .addProblemChange(problemId, (workingSolution, problemChangeDirector) -> {
                        problemChangeDirector.addProblemFact(new TestdataValue("addedValue"),
                                workingSolution.getValueList()::add);
                    });

            futureChange.get();
            assertThat(futureChange).isCompleted();
            assertThat(bestSolution.get().getValueList()).hasSize(entityAndValueCount + 1);
        }
    }

    @Test
    @Timeout(60)
    void addProblemChangeToNonExistingProblem_failsFast() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        try (var solverManager = createDefaultSolverManager(solverConfig)) {
            solverManager.solveBuilder()
                    .withProblemId(1L)
                    .withProblemFinder(id -> PlannerTestUtils.generateTestdataSolution("s1", 4))
                    .withBestSolutionConsumer(testdataSolution -> {
                    })
                    .run();

            var nonExistingProblemId = 999L;
            assertThatIllegalStateException()
                    .isThrownBy(() -> solverManager.addProblemChange(nonExistingProblemId,
                            (workingSolution, problemChangeDirector) -> problemChangeDirector.addProblemFact(
                                    new TestdataValue("addedValue"),
                                    workingSolution.getValueList()::add)))
                    .withMessageContaining(String.valueOf(nonExistingProblemId));
        }
    }

    @Test
    @Timeout(60)
    void addProblemChangeToWaitingSolver() throws InterruptedException, ExecutionException {
        var solvingPausedLatch = new CountDownLatch(1);
        var pausedPhaseConfig = new CustomPhaseConfig().withCustomPhaseCommands(
                (ScoreDirector<TestdataSolution> scoreDirector, BooleanSupplier booleanSupplier) -> {
                    try {
                        solvingPausedLatch.await();
                    } catch (InterruptedException e) {
                        fail("CountDownLatch failed.");
                    }
                });

        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(pausedPhaseConfig, new ConstructionHeuristicPhaseConfig());
        // Allow only a single active solver.
        try (var solverManager = createSolverManagerWithOneSolver(solverConfig)) {
            // The first solver waits until the test sends a problem change.
            solverManager.solve(1L, PlannerTestUtils.generateTestdataSolution("s1", 4));

            // The second solver is scheduled and waits for the fist solver to finish.
            var secondProblemId = 2L;
            var entityAndValueCount = 4;
            var bestSolution = new AtomicReference<TestdataSolution>();
            solverManager.solveBuilder()
                    .withProblemId(secondProblemId)
                    .withProblemFinder(id -> PlannerTestUtils.generateTestdataSolution("s2", entityAndValueCount))
                    .withBestSolutionConsumer(bestSolution::set)
                    .run();

            var futureChange = solverManager
                    .addProblemChange(secondProblemId, (workingSolution, problemChangeDirector) -> {
                        problemChangeDirector.addProblemFact(new TestdataValue("addedValue"),
                                workingSolution.getValueList()::add);
                    });

            // The first solver can proceed. When it finishes, the second solver starts solving and picks up the change.
            solvingPausedLatch.countDown();
            futureChange.get();
            assertThat(futureChange).isCompleted();
            assertThat(bestSolution.get().getValueList()).hasSize(entityAndValueCount + 1);
        }
    }

    @Test
    @Timeout(60)
    void terminateSolverJobEarly_stillReturnsBestSolution() throws ExecutionException, InterruptedException {
        var solvingStartedLatch = new CountDownLatch(1);
        var pausedPhaseConfig = new CustomPhaseConfig()
                .withCustomPhaseCommands((ScoreDirector<TestdataSolution> scoreDirector,
                        BooleanSupplier booleanSupplier) -> solvingStartedLatch.countDown());

        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(pausedPhaseConfig, new ConstructionHeuristicPhaseConfig());
        try (var solverManager = createDefaultSolverManager(solverConfig)) {
            var solverJob = solverManager.solve(1L, PlannerTestUtils.generateTestdataSolution("s1"));
            solvingStartedLatch.await();
            solverJob.terminateEarly();
            var result = solverJob.getFinalBestSolution();
            assertThat(result).isNotNull();
            assertThat(solverJob.isTerminatedEarly()).isTrue();
        }
    }

    @Test
    @Timeout(60)
    void terminateScheduledSolverJobEarly_returnsInputProblem() throws ExecutionException, InterruptedException {
        var solvingPausedLatch = new CountDownLatch(1);
        var pausedPhaseConfig = new CustomPhaseConfig().withCustomPhaseCommands(
                (ScoreDirector<TestdataSolution> scoreDirector, BooleanSupplier booleanSupplier) -> {
                    try {
                        solvingPausedLatch.await();
                    } catch (InterruptedException e) {
                        fail("CountDownLatch failed.");
                    }
                });

        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(pausedPhaseConfig, new ConstructionHeuristicPhaseConfig());
        // Allow only a single active solver.
        try (var solverManager = createSolverManagerWithOneSolver(solverConfig)) {
            // The first solver waits.
            solverManager.solve(1L, PlannerTestUtils.generateTestdataSolution("s1", 4));
            var inputProblem = PlannerTestUtils.generateTestdataSolution("s2", 4);
            var solverJob = solverManager.solve(2L, inputProblem);

            solverJob.terminateEarly();
            var result = solverJob.getFinalBestSolution();
            assertThat(result).isSameAs(inputProblem);
            assertThat(solverJob.isTerminatedEarly()).isTrue();
        }
    }

    public static class CustomThreadFactory implements ThreadFactory {
        private static final String CUSTOM_THREAD_NAME = "CustomThread";

        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            return new Thread(runnable, CUSTOM_THREAD_NAME);
        }
    }

    @Test
    @Timeout(60)
    void threadFactoryIsUsed() throws ExecutionException, InterruptedException {
        var threadCheckingPhaseConfig = new CustomPhaseConfig().withCustomPhaseCommands(
                (ScoreDirector<TestdataSolution> scoreDirector, BooleanSupplier booleanSupplier) -> {
                    if (!Thread.currentThread().getName().equals(CustomThreadFactory.CUSTOM_THREAD_NAME)) {
                        fail("Custom thread factory not used");
                    }
                });

        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(threadCheckingPhaseConfig, new ConstructionHeuristicPhaseConfig());

        var solverManagerConfig = new SolverManagerConfig().withThreadFactoryClass(CustomThreadFactory.class);
        try (var solverManager = createSolverManager(solverConfig, solverManagerConfig)) {
            var inputProblem = PlannerTestUtils.generateTestdataSolution("s1", 4);
            var solverJob = solverManager.solve(1L, inputProblem);
            var result = solverJob.getFinalBestSolution();
            assertThat(result).isNotNull();
        }
    }
}
