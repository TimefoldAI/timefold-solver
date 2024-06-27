package ai.timefold.solver.core.api.solver;

import static ai.timefold.solver.core.api.solver.SolverStatus.NOT_SOLVING;
import static ai.timefold.solver.core.api.solver.SolverStatus.SOLVING_ACTIVE;
import static ai.timefold.solver.core.api.solver.SolverStatus.SOLVING_SCHEDULED;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertSolutionInitialized;
import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.solver.DefaultSolverJob;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.extended.TestdataUnannotatedExtendedSolution;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class SolverManagerTest {

    private SolverManager<TestdataSolution, Long> solverManager;

    @AfterEach
    void closeSolverManager() {
        if (solverManager != null) {
            solverManager.close();
        }
    }

    @Test
    void create() {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        SolverManager.create(solverConfig).close();
        SolverManagerConfig solverManagerConfig = new SolverManagerConfig();
        SolverManager.create(solverConfig, solverManagerConfig).close();
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
        SolverManager.create(solverFactory).close();
        SolverManager.create(solverFactory, solverManagerConfig).close();
    }

    @Test
    @Timeout(60)
    void solveBatch_2InParallel() throws ExecutionException, InterruptedException {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(createPhaseWithConcurrentSolvingStart(2), new ConstructionHeuristicPhaseConfig());
        solverManager = SolverManager.create(
                solverConfig, new SolverManagerConfig().withParallelSolverCount("2"));

        SolverJob<TestdataSolution, Long> solverJob1 = solverManager.solve(1L,
                PlannerTestUtils.generateTestdataSolution("s1"));
        SolverJob<TestdataSolution, Long> solverJob2 = solverManager.solve(2L,
                PlannerTestUtils.generateTestdataSolution("s2"));

        assertSolutionInitialized(solverJob1.getFinalBestSolution());
        assertSolutionInitialized(solverJob2.getFinalBestSolution());
    }

    private CustomPhaseConfig createPhaseWithConcurrentSolvingStart(int barrierPartiesCount) {
        CyclicBarrier barrier = new CyclicBarrier(barrierPartiesCount);
        return new CustomPhaseConfig().withCustomPhaseCommands(
                scoreDirector -> {
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
        CyclicBarrier solverThreadReadyBarrier = new CyclicBarrier(2);
        CyclicBarrier mainThreadReadyBarrier = new CyclicBarrier(2);
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new CustomPhaseConfig().withCustomPhaseCommands(
                        scoreDirector -> {
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
        solverManager = SolverManager.create(
                solverConfig, new SolverManagerConfig().withParallelSolverCount("1"));

        SolverJob<TestdataSolution, Long> solverJob1 = solverManager.solve(1L,
                PlannerTestUtils.generateTestdataSolution("s1"));
        solverThreadReadyBarrier.await();
        SolverJob<TestdataSolution, Long> solverJob2 = solverManager.solve(2L,
                PlannerTestUtils.generateTestdataSolution("s2"));
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

    @Test
    @Timeout(60)
    void exceptionInSolver() {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new CustomPhaseConfig().withCustomPhaseCommands(
                        scoreDirector -> {
                            throw new IllegalStateException("exceptionInSolver");
                        }));
        solverManager = SolverManager.create(
                solverConfig, new SolverManagerConfig().withParallelSolverCount("1"));

        AtomicInteger exceptionCount = new AtomicInteger();
        SolverJob<TestdataSolution, Long> solverJob1 = solverManager.solveBuilder()
                .withProblemId(1L)
                .withProblemFinder(problemId -> PlannerTestUtils.generateTestdataSolution("s1"))
                .withExceptionHandler((problemId, throwable) -> exceptionCount.incrementAndGet())
                .run();
        assertThatThrownBy(solverJob1::getFinalBestSolution)
                .isInstanceOf(ExecutionException.class)
                .hasRootCauseMessage("exceptionInSolver");
        assertThat(exceptionCount.get()).isEqualTo(1);
        assertThat(solverManager.getSolverStatus(1L)).isEqualTo(NOT_SOLVING);
        assertThat(solverJob1.getSolverStatus()).isEqualTo(NOT_SOLVING);
    }

    @Test
    @Timeout(60)
    void errorThrowableInSolver() {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new CustomPhaseConfig().withCustomPhaseCommands(
                        scoreDirector -> {
                            throw new OutOfMemoryError("exceptionInSolver");
                        }));
        solverManager = SolverManager.create(
                solverConfig, new SolverManagerConfig().withParallelSolverCount("1"));

        AtomicInteger exceptionCount = new AtomicInteger();
        SolverJob<TestdataSolution, Long> solverJob1 = solverManager.solveBuilder()
                .withProblemId(1L)
                .withProblemFinder(problemId -> PlannerTestUtils.generateTestdataSolution("s1"))
                .withExceptionHandler((problemId, throwable) -> exceptionCount.incrementAndGet())
                .run();
        assertThatThrownBy(solverJob1::getFinalBestSolution)
                .isInstanceOf(ExecutionException.class)
                .hasRootCauseMessage("exceptionInSolver");
        assertThat(exceptionCount.get()).isEqualTo(1);
        assertThat(solverManager.getSolverStatus(1L)).isEqualTo(NOT_SOLVING);
        assertThat(solverJob1.getSolverStatus()).isEqualTo(NOT_SOLVING);
    }

    @Test
    @Timeout(60)
    void exceptionInConsumer() throws InterruptedException {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());
        solverManager = SolverManager.create(
                solverConfig, new SolverManagerConfig().withParallelSolverCount("1"));

        CountDownLatch consumerInvoked = new CountDownLatch(1);
        AtomicReference<Throwable> errorInConsumer = new AtomicReference<>();
        SolverJob<TestdataSolution, Long> solverJob1 = solverManager.solveBuilder()
                .withProblemId(1L)
                .withProblemFinder(problemId -> PlannerTestUtils.generateTestdataSolution("s1"))
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

    @Test
    @Timeout(60)
    void solveGenerics() throws ExecutionException, InterruptedException {
        SolverConfig solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        solverManager = SolverManager
                .create(solverConfig, new SolverManagerConfig());

        BiConsumer<Object, Object> exceptionHandler = (o1, o2) -> fail("Solving failed.");
        Consumer<Object> finalBestSolutionConsumer = o -> {
        };
        Function<Object, TestdataUnannotatedExtendedSolution> problemFinder = o -> new TestdataUnannotatedExtendedSolution(
                PlannerTestUtils.generateTestdataSolution("s1"));

        SolverJob<TestdataSolution, Long> solverJob = solverManager.solveBuilder()
                .withProblemId(1L)
                .withProblemFinder(problemFinder)
                .withFinalBestSolutionConsumer(finalBestSolutionConsumer)
                .withExceptionHandler(exceptionHandler)
                .run();
        solverJob.getFinalBestSolution();
    }

    @Test
    @Timeout(60)
    void firstInitializedSolutionConsumerWithDefaultPhases() throws ExecutionException, InterruptedException {
        MutableBoolean hasInitializedSolution = new MutableBoolean();
        Consumer<Object> initializedSolutionConsumer = ignore -> hasInitializedSolution.setTrue();

        // Default configuration
        SolverConfig solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withTerminationConfig(new TerminationConfig()
                        .withUnimprovedMillisecondsSpentLimit(1L));
        solverManager = SolverManager
                .create(solverConfig, new SolverManagerConfig());
        Function<Object, TestdataUnannotatedExtendedSolution> problemFinder = o -> new TestdataUnannotatedExtendedSolution(
                PlannerTestUtils.generateTestdataSolution("s1"));

        SolverJob<TestdataSolution, Long> solverJob = solverManager.solveBuilder()
                .withProblemId(1L)
                .withProblemFinder(problemFinder)
                .withFirstInitializedSolutionConsumer(initializedSolutionConsumer)
                .run();
        solverJob.getFinalBestSolution();
        assertThat(hasInitializedSolution.booleanValue()).isTrue();
    }

    @Test
    @Timeout(60)
    void firstInitializedSolutionConsumerWithSinglePhase() throws ExecutionException, InterruptedException {
        MutableBoolean hasInitializedSolution = new MutableBoolean();
        Consumer<Object> initializedSolutionConsumer = ignore -> hasInitializedSolution.setTrue();

        // Only CH
        SolverConfig solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());
        solverManager = SolverManager
                .create(solverConfig, new SolverManagerConfig());
        Function<Object, TestdataUnannotatedExtendedSolution> problemFinder = o -> new TestdataUnannotatedExtendedSolution(
                PlannerTestUtils.generateTestdataSolution("s1"));

        SolverJob<TestdataSolution, Long> solverJob = solverManager.solveBuilder()
                .withProblemId(1L)
                .withProblemFinder(problemFinder)
                .withFirstInitializedSolutionConsumer(initializedSolutionConsumer)
                .run();
        solverJob.getFinalBestSolution();
        assertThat(hasInitializedSolution.booleanValue()).isFalse();
        hasInitializedSolution.setFalse();

        // Only LS
        solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new LocalSearchPhaseConfig())
                .withTerminationConfig(new TerminationConfig()
                        .withBestScoreLimit("0"));
        solverManager = SolverManager
                .create(solverConfig, new SolverManagerConfig());
        TestdataUnannotatedExtendedSolution initializedSolution =
                new TestdataUnannotatedExtendedSolution(PlannerTestUtils.generateTestdataSolution("s1"));
        initializedSolution.getEntityList().forEach(e -> e.setValue(initializedSolution.getValueList().get(0)));

        solverJob = solverManager.solveBuilder()
                .withProblemId(1L)
                .withProblemFinder(o -> initializedSolution)
                .withFirstInitializedSolutionConsumer(initializedSolutionConsumer)
                .withFinalBestSolutionConsumer(ignore -> {
                })
                .run();
        solverJob.getFinalBestSolution();
        assertThat(hasInitializedSolution.booleanValue()).isFalse();
    }

    @Test
    @Timeout(60)
    void firstInitializedSolutionConsumerWith2CHAndLS() throws ExecutionException, InterruptedException {
        MutableBoolean hasInitializedSolution = new MutableBoolean();
        Consumer<Object> initializedSolutionConsumer = ignore -> hasInitializedSolution.setTrue();

        // CH - CH - LS
        SolverConfig solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig(), new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig())
                .withTerminationConfig(new TerminationConfig()
                        .withUnimprovedMillisecondsSpentLimit(1L));
        hasInitializedSolution.setFalse();
        solverManager = SolverManager
                .create(solverConfig, new SolverManagerConfig());
        Function<Object, TestdataUnannotatedExtendedSolution> problemFinder = o -> new TestdataUnannotatedExtendedSolution(
                PlannerTestUtils.generateTestdataSolution("s1"));

        SolverJob<TestdataSolution, Long> solverJob = solverManager.solveBuilder()
                .withProblemId(1L)
                .withProblemFinder(problemFinder)
                .withFirstInitializedSolutionConsumer(initializedSolutionConsumer)
                .run();
        solverJob.getFinalBestSolution();
        assertThat(hasInitializedSolution.booleanValue()).isTrue();
    }

    @Test
    @Timeout(60)
    void firstInitializedSolutionConsumerWithCustomAndCHAndLS() throws ExecutionException, InterruptedException {
        MutableBoolean hasInitializedSolution = new MutableBoolean();
        Consumer<Object> initializedSolutionConsumer = ignore -> hasInitializedSolution.setTrue();

        // CS - CH - LS
        SolverConfig solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new CustomPhaseConfig()
                        .withCustomPhaseCommandList(List.of(scoreDirector -> {
                            assertThat(hasInitializedSolution.booleanValue()).isFalse();
                        })),
                        new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig())
                .withTerminationConfig(new TerminationConfig()
                        .withUnimprovedMillisecondsSpentLimit(1L));
        solverManager = SolverManager
                .create(solverConfig, new SolverManagerConfig());
        Function<Object, TestdataUnannotatedExtendedSolution> problemFinder = o -> new TestdataUnannotatedExtendedSolution(
                PlannerTestUtils.generateTestdataSolution("s1"));

        SolverJob<TestdataSolution, Long> solverJob = solverManager.solveBuilder()
                .withProblemId(1L)
                .withProblemFinder(problemFinder)
                .withFirstInitializedSolutionConsumer(initializedSolutionConsumer)
                .run();
        solverJob.getFinalBestSolution();
        assertThat(hasInitializedSolution.booleanValue()).isTrue();
    }

    @Test
    @Timeout(60)
    void firstInitializedSolutionConsumerWithCHAndCustomAndLS() throws ExecutionException, InterruptedException {
        MutableBoolean hasInitializedSolution = new MutableBoolean();
        Consumer<Object> initializedSolutionConsumer = ignore -> hasInitializedSolution.setTrue();

        // CH - CS - LS
        SolverConfig solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(
                        new ConstructionHeuristicPhaseConfig(),
                        new CustomPhaseConfig()
                                .withCustomPhaseCommandList(List.of(scoreDirector -> {
                                    assertThat(hasInitializedSolution.booleanValue()).isFalse();
                                })),
                        new LocalSearchPhaseConfig())
                .withTerminationConfig(new TerminationConfig()
                        .withUnimprovedMillisecondsSpentLimit(1L));
        solverManager = SolverManager
                .create(solverConfig, new SolverManagerConfig());
        Function<Object, TestdataUnannotatedExtendedSolution> problemFinder = o -> new TestdataUnannotatedExtendedSolution(
                PlannerTestUtils.generateTestdataSolution("s1"));

        SolverJob<TestdataSolution, Long> solverJob = solverManager.solveBuilder()
                .withProblemId(1L)
                .withProblemFinder(problemFinder)
                .withFirstInitializedSolutionConsumer(initializedSolutionConsumer)
                .run();
        solverJob.getFinalBestSolution();
        assertThat(hasInitializedSolution.booleanValue()).isTrue();
    }

    @Test
    @Timeout(60)
    void firstInitializedSolutionConsumerWith2Custom() throws ExecutionException, InterruptedException {
        MutableBoolean hasInitializedSolution = new MutableBoolean();
        Consumer<Object> initializedSolutionConsumer = ignore -> hasInitializedSolution.setTrue();

        // CS (CH) - CS (LS)
        SolverConfig solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(
                        new CustomPhaseConfig()
                                .withCustomPhaseCommandList(List.of(scoreDirector -> {
                                    assertThat(hasInitializedSolution.booleanValue()).isFalse();
                                })),
                        new CustomPhaseConfig()
                                .withCustomPhaseCommandList(List.of(scoreDirector -> {
                                    assertThat(hasInitializedSolution.booleanValue()).isFalse();
                                })))
                .withTerminationConfig(new TerminationConfig()
                        .withUnimprovedMillisecondsSpentLimit(1L));
        solverManager = SolverManager
                .create(solverConfig, new SolverManagerConfig());
        Function<Object, TestdataUnannotatedExtendedSolution> problemFinder = o -> new TestdataUnannotatedExtendedSolution(
                PlannerTestUtils.generateTestdataSolution("s1"));

        SolverJob<TestdataSolution, Long> solverJob = solverManager.solveBuilder()
                .withProblemId(1L)
                .withProblemFinder(problemFinder)
                .withFirstInitializedSolutionConsumer(initializedSolutionConsumer)
                .run();
        solverJob.getFinalBestSolution();
        assertThat(hasInitializedSolution.booleanValue()).isFalse();
    }

    @Test
    void solveWithOverride() {
        // Default spent limit is 1L
        TerminationConfig terminationConfig = new TerminationConfig()
                .withSpentLimit(Duration.ofSeconds(1L));
        SolverConfig solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withTerminationConfig(terminationConfig);
        solverConfig.withTerminationConfig(terminationConfig);

        solverManager = SolverManager
                .create(solverConfig, new SolverManagerConfig());

        TestdataUnannotatedExtendedSolution problem =
                new TestdataUnannotatedExtendedSolution(PlannerTestUtils.generateTestdataSolution("s1"));

        SolverScope<TestdataSolution> solverScope = mock(SolverScope.class);
        doReturn(50L).when(solverScope).calculateTimeMillisSpentUpToNow();

        DefaultSolverJob<TestdataSolution, Long> solverJob =
                (DefaultSolverJob<TestdataSolution, Long>) solverManager.solve(1L, problem);
        assertThat(solverJob.getSolverTermination().calculateSolverTimeGradient(solverScope)).isEqualTo(0.05);

        // Spent limit overridden by 100L
        SolverConfigOverride<TestdataSolution> configOverride = new SolverConfigOverride<TestdataSolution>()
                .withTerminationConfig(new TerminationConfig().withSpentLimit(Duration.ofMillis(100L)));
        solverJob = (DefaultSolverJob<TestdataSolution, Long>) solverManager.solveBuilder()
                .withProblemId(2L)
                .withProblem(problem)
                .withConfigOverride(configOverride)
                .run();
        assertThat(solverJob.getSolverTermination().calculateSolverTimeGradient(solverScope)).isEqualTo(0.5);
    }

    @Test
    void testScoreCalculationCountForFinishedJob() throws ExecutionException, InterruptedException {
        // Terminate after exactly 5 score calculations
        var terminationConfig = new TerminationConfig()
                .withScoreCalculationCountLimit(5L);
        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withTerminationConfig(terminationConfig);

        solverManager = SolverManager
                .create(solverConfig, new SolverManagerConfig());

        var problem = PlannerTestUtils.generateTestdataSolution("s1");
        var solverJob = (DefaultSolverJob<TestdataSolution, Long>) solverManager.solveBuilder()
                .withProblemId(2L)
                .withProblem(problem)
                .run();

        solverJob.getFinalBestSolution();
        assertThat(solverJob.getScoreCalculationCount()).isEqualTo(5L);

        // Score calculation speed and solve duration are non-deterministic.
        // On an exceptionally fast machine, getSolvingDuration() can return Duration.ZERO.
        // On an exceptionally slow machine, getScoreCalculationSpeed() can be 0 due to flooring
        // (i.e. by taking more than 5 seconds to finish solving).
        assertThat(solverJob.getSolvingDuration()).isGreaterThanOrEqualTo(Duration.ZERO);
        assertThat(solverJob.getScoreCalculationSpeed()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void testProblemSizeStatisticsForFinishedJob() throws ExecutionException, InterruptedException {
        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class);

        solverManager = SolverManager
                .create(solverConfig, new SolverManagerConfig());

        var problem = PlannerTestUtils.generateTestdataSolution("s1", 2);
        var solverJob = (DefaultSolverJob<TestdataSolution, Long>) solverManager.solveBuilder()
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

    @Test
    @Timeout(60)
    void testProblemSizeStatisticsForWaitingJob() throws InterruptedException, ExecutionException {
        CountDownLatch solvingPausedLatch = new CountDownLatch(1);
        PhaseConfig<?> pausedPhaseConfig = new CustomPhaseConfig().withCustomPhaseCommands(
                scoreDirector -> {
                    try {
                        solvingPausedLatch.await();
                    } catch (InterruptedException e) {
                        fail("CountDownLatch failed.");
                    }
                });

        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(pausedPhaseConfig, new ConstructionHeuristicPhaseConfig());
        // Allow only a single active solver.
        SolverManagerConfig solverManagerConfig = new SolverManagerConfig().withParallelSolverCount("1");
        solverManager = SolverManager.create(solverConfig, solverManagerConfig);

        // The first solver waits until the test sends a problem change.
        solverManager.solve(1L, PlannerTestUtils.generateTestdataSolution("s1", 4));

        // The second solver is scheduled and waits for the fist solver to finish.
        final long secondProblemId = 2L;
        final int entityAndValueCount = 4;
        AtomicReference<TestdataSolution> bestSolution = new AtomicReference<>();
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

        CompletableFuture<Void> futureChange = solverManager
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

        doCallRealMethod().when(solverManager).solve(any(Long.class), any(TestdataSolution.class), any(Consumer.class),
                any(BiConsumer.class));
        solverManager.solve(1L, mock(TestdataSolution.class), mock(Consumer.class), mock(BiConsumer.class));
        verify(solverJobBuilder, times(3)).withProblemId(anyLong());
        verify(solverJobBuilder, times(3)).withProblem(any());
        verify(solverJobBuilder, times(2)).withFinalBestSolutionConsumer(any());
        verify(solverJobBuilder, times(1)).withExceptionHandler(any());

        doCallRealMethod().when(solverManager).solve(any(Long.class), any(Function.class), any(Consumer.class));
        solverManager.solve(1L, mock(Function.class), mock(Consumer.class));
        verify(solverJobBuilder, times(4)).withProblemId(anyLong());
        verify(solverJobBuilder, times(1)).withProblemFinder(any());
        verify(solverJobBuilder, times(3)).withFinalBestSolutionConsumer(any());

        doCallRealMethod().when(solverManager).solve(any(Long.class), any(Function.class), any(Consumer.class),
                any(BiConsumer.class));
        solverManager.solve(1L, mock(Function.class), mock(Consumer.class), mock(BiConsumer.class));
        verify(solverJobBuilder, times(5)).withProblemId(anyLong());
        verify(solverJobBuilder, times(2)).withProblemFinder(any());
        verify(solverJobBuilder, times(4)).withFinalBestSolutionConsumer(any());
        verify(solverJobBuilder, times(2)).withExceptionHandler(any());

        doCallRealMethod().when(solverManager).solveAndListen(any(Long.class), any(Function.class), any(Consumer.class));
        solverManager.solveAndListen(1L, mock(Function.class), mock(Consumer.class));
        verify(solverJobBuilder, times(6)).withProblemId(anyLong());
        verify(solverJobBuilder, times(3)).withProblemFinder(any());
        verify(solverJobBuilder, times(1)).withBestSolutionConsumer(any());

        doCallRealMethod().when(solverManager).solveAndListen(any(Long.class), any(TestdataSolution.class),
                any(Consumer.class));
        solverManager.solveAndListen(1L, mock(TestdataSolution.class), mock(Consumer.class));
        verify(solverJobBuilder, times(7)).withProblemId(anyLong());
        verify(solverJobBuilder, times(4)).withProblem(any());
        verify(solverJobBuilder, times(2)).withBestSolutionConsumer(any());

        doCallRealMethod().when(solverManager).solveAndListen(any(Long.class), any(Function.class), any(Consumer.class),
                any(BiConsumer.class));
        solverManager.solveAndListen(1L, mock(Function.class), mock(Consumer.class), mock(BiConsumer.class));
        verify(solverJobBuilder, times(8)).withProblemId(anyLong());
        verify(solverJobBuilder, times(4)).withProblemFinder(any());
        verify(solverJobBuilder, times(3)).withBestSolutionConsumer(any());
        verify(solverJobBuilder, times(3)).withExceptionHandler(any());

        doCallRealMethod().when(solverManager).solveAndListen(any(Long.class), any(Function.class), any(Consumer.class),
                any(Consumer.class), any(BiConsumer.class));
        solverManager.solveAndListen(1L, mock(Function.class), mock(Consumer.class), mock(Consumer.class),
                mock(BiConsumer.class));
        verify(solverJobBuilder, times(9)).withProblemId(anyLong());
        verify(solverJobBuilder, times(5)).withProblemFinder(any());
        verify(solverJobBuilder, times(4)).withBestSolutionConsumer(any());
        verify(solverJobBuilder, times(5)).withFinalBestSolutionConsumer(any());
        verify(solverJobBuilder, times(4)).withExceptionHandler(any());
    }

    @Test
    @Timeout(60)
    void solveWithBuilder() throws InterruptedException, BrokenBarrierException {
        CyclicBarrier startedBarrier = new CyclicBarrier(2);
        SolverConfig solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        solverManager = SolverManager
                .create(solverConfig, new SolverManagerConfig());

        BiConsumer<Object, Object> exceptionHandler = (o1, o2) -> fail("Solving failed.");
        MutableObject finalBestSolution = new MutableObject();
        Consumer<Object> finalBestConsumer = o -> {
            finalBestSolution.setValue(o);
            try {
                startedBarrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
        };
        Function<Object, TestdataUnannotatedExtendedSolution> problemFinder = o -> new TestdataUnannotatedExtendedSolution(
                PlannerTestUtils.generateTestdataSolution("s1"));

        solverManager.solveBuilder()
                .withProblemId(1L)
                .withProblemFinder(problemFinder)
                .withFinalBestSolutionConsumer(finalBestConsumer)
                .withExceptionHandler(exceptionHandler)
                .run();

        startedBarrier.await();
        assertThat(finalBestSolution.getValue()).isNotNull();
    }

    @Test
    @Timeout(60)
    void solveAndListenWithBuilder() throws InterruptedException, BrokenBarrierException {
        CyclicBarrier startedBarrier = new CyclicBarrier(2);
        SolverConfig solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        solverManager = SolverManager
                .create(solverConfig, new SolverManagerConfig());

        BiConsumer<Object, Object> exceptionHandler = (o1, o2) -> fail("Solving failed.");
        MutableObject finalBestSolution = new MutableObject();
        Consumer<Object> finalBestConsumer = o -> {
            finalBestSolution.setValue(o);
            try {
                startedBarrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
        };
        MutableObject bestSolution = new MutableObject();
        Consumer<Object> bestConsumer = o -> {
            bestSolution.setValue(o);
        };
        Function<Object, TestdataUnannotatedExtendedSolution> problemFinder = o -> new TestdataUnannotatedExtendedSolution(
                PlannerTestUtils.generateTestdataSolution("s1"));

        solverManager.solveBuilder()
                .withProblemId(1L)
                .withProblemFinder(problemFinder)
                .withFinalBestSolutionConsumer(finalBestConsumer)
                .withBestSolutionConsumer(bestConsumer)
                .withExceptionHandler(exceptionHandler)
                .run();

        startedBarrier.await();
        assertThat(finalBestSolution.getValue()).isNotNull();
        assertThat(bestSolution.getValue()).isNotNull();
    }

    @Test
    @Timeout(60)
    void skipAhead() throws ExecutionException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class,
                TestdataEntity.class)
                .withPhases(new CustomPhaseConfig().withCustomPhaseCommands(
                        (ScoreDirector<TestdataSolution> scoreDirector) -> {
                            TestdataSolution solution = scoreDirector.getWorkingSolution();
                            TestdataEntity entity = solution.getEntityList().get(0);
                            scoreDirector.beforeVariableChanged(entity, "value");
                            entity.setValue(solution.getValueList().get(0));
                            scoreDirector.afterVariableChanged(entity, "value");
                            scoreDirector.triggerVariableListeners();
                        }, (ScoreDirector<TestdataSolution> scoreDirector) -> {
                            TestdataSolution solution = scoreDirector.getWorkingSolution();
                            TestdataEntity entity = solution.getEntityList().get(1);
                            scoreDirector.beforeVariableChanged(entity, "value");
                            entity.setValue(solution.getValueList().get(1));
                            scoreDirector.afterVariableChanged(entity, "value");
                            scoreDirector.triggerVariableListeners();
                        }, (ScoreDirector<TestdataSolution> scoreDirector) -> {
                            TestdataSolution solution = scoreDirector.getWorkingSolution();
                            TestdataEntity entity = solution.getEntityList().get(2);
                            scoreDirector.beforeVariableChanged(entity, "value");
                            entity.setValue(solution.getValueList().get(2));
                            scoreDirector.afterVariableChanged(entity, "value");
                            scoreDirector.triggerVariableListeners();
                        }, (ScoreDirector<TestdataSolution> scoreDirector) -> {
                            // In the next best solution event, both e1 and e2 are definitely not null (but e3 might be).
                            latch.countDown();
                            TestdataSolution solution = scoreDirector.getWorkingSolution();
                            TestdataEntity entity = solution.getEntityList().get(3);
                            scoreDirector.beforeVariableChanged(entity, "value");
                            entity.setValue(solution.getValueList().get(3));
                            scoreDirector.afterVariableChanged(entity, "value");
                            scoreDirector.triggerVariableListeners();
                        }));
        solverManager = SolverManager.create(
                solverConfig, new SolverManagerConfig().withParallelSolverCount("1"));
        AtomicInteger bestSolutionCount = new AtomicInteger();
        AtomicInteger finalBestSolutionCount = new AtomicInteger();
        AtomicReference<Throwable> consumptionError = new AtomicReference<>();
        CountDownLatch finalBestSolutionConsumed = new CountDownLatch(1);
        SolverJob<TestdataSolution, Long> solverJob1 = solverManager.solveBuilder()
                .withProblemId(1L)
                .withProblemFinder(problemId -> PlannerTestUtils.generateTestdataSolution("s1", 4))
                .withBestSolutionConsumer(bestSolution -> {
                    boolean isFirstReceivedSolution = bestSolutionCount.incrementAndGet() == 1;
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
        assertSolutionInitialized(solverJob1.getFinalBestSolution());
        // EventCount can be 2 or 3, depending on the race, but it can never be 4.
        assertThat(bestSolutionCount).hasValueLessThan(4);
        finalBestSolutionConsumed.await();
        assertThat(finalBestSolutionCount.get()).isEqualTo(1);
        if (consumptionError.get() != null) {
            fail("Error in the best solution consumer.", consumptionError.get());
        }
    }

    @Test
    @Timeout(600)
    void terminateEarly() throws InterruptedException, BrokenBarrierException {
        CyclicBarrier startedBarrier = new CyclicBarrier(2);
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class,
                TestdataEntity.class)
                .withTerminationConfig(new TerminationConfig())
                .withPhases(new CustomPhaseConfig().withCustomPhaseCommands((scoreDirector) -> {
                    try {
                        startedBarrier.await();
                    } catch (InterruptedException | BrokenBarrierException e) {
                        throw new IllegalStateException("The startedBarrier failed.", e);
                    }
                }),
                        new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig());

        solverManager = SolverManager.create(
                solverConfig, new SolverManagerConfig().withParallelSolverCount("1"));

        SolverJob<TestdataSolution, Long> solverJob1 = solverManager.solve(1L,
                PlannerTestUtils.generateTestdataSolution("s1", 4));
        SolverJob<TestdataSolution, Long> solverJob2 = solverManager.solve(2L,
                PlannerTestUtils.generateTestdataSolution("s2", 4));
        SolverJob<TestdataSolution, Long> solverJob3 = solverManager.solve(3L,
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

    /**
     * Tests whether SolverManager can solve on multiple threads problems that use multiple thread counts.
     */
    @Disabled("https://issues.redhat.com/browse/PLANNER-1837")
    @Test
    @Timeout(60)
    void solveMultipleThreadedMovesWithSolverManager_allGetSolved() throws ExecutionException, InterruptedException {
        int processCount = Runtime.getRuntime().availableProcessors();
        SolverConfig solverConfig =
                PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                        .withPhases(new ConstructionHeuristicPhaseConfig(), new LocalSearchPhaseConfig())
                        //                .withTerminationConfig(new TerminationConfig().withSecondsSpentLimit(4L))
                        // Adds moveThreadCount to the solver config.
                        .withMoveThreadCount("AUTO");
        // Creates solverManagerConfig with multiple threads.
        solverManager =
                SolverManager.create(solverConfig, new SolverManagerConfig());

        List<SolverJob<TestdataSolution, Long>> jobs = new ArrayList<>();
        for (long i = 0; i < processCount; i++) {
            jobs.add(solverManager.solve(i, PlannerTestUtils.generateTestdataSolution("s" + i, 10)));
        }

        assertInitializedJobs(jobs);
    }

    private void assertInitializedJobs(List<SolverJob<TestdataSolution, Long>> jobs)
            throws InterruptedException, ExecutionException {
        for (SolverJob<TestdataSolution, Long> job : jobs) {
            // Method getFinalBestSolution() waits for the solving to finish, therefore it ensures synchronization.
            assertSolutionInitialized(job.getFinalBestSolution());
        }
    }

    @Test
    @Timeout(60)
    void submitMoreProblemsThanCpus_allGetSolved() throws InterruptedException, ExecutionException {
        // Use twice the amount of problems than available processors.
        int problemCount = Runtime.getRuntime().availableProcessors() * 2;

        solverManager = createSolverManagerTestableByDifferentConsumers();
        assertSolveWithoutConsumer(problemCount, solverManager);
        assertSolveWithConsumer(problemCount, solverManager, true);
        assertSolveWithConsumer(problemCount, solverManager, false);
    }

    private SolverManager<TestdataSolution, Long> createSolverManagerTestableByDifferentConsumers() {
        List<PhaseConfig> phaseConfigList = IntStream.of(0, 1)
                .mapToObj((x) -> new CustomPhaseConfig().withCustomPhaseCommands(
                        (ScoreDirector<TestdataSolution> scoreDirector) -> {
                            TestdataSolution solution = scoreDirector.getWorkingSolution();
                            TestdataEntity entity = solution.getEntityList().get(x);
                            scoreDirector.beforeVariableChanged(entity, "value");
                            entity.setValue(solution.getValueList().get(x));
                            scoreDirector.afterVariableChanged(entity, "value");
                            scoreDirector.triggerVariableListeners();
                        }))
                .collect(Collectors.toList());

        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class,
                TestdataEntity.class)
                .withPhases(phaseConfigList.toArray(new PhaseConfig[0]));

        SolverManagerConfig solverManagerConfig = new SolverManagerConfig();

        return SolverManager.create(solverConfig, solverManagerConfig);
    }

    private void assertSolveWithoutConsumer(int problemCount, SolverManager<TestdataSolution, Long> solverManager)
            throws InterruptedException, ExecutionException {
        List<SolverJob<TestdataSolution, Long>> jobs = new ArrayList<>(problemCount);

        for (long id = 0; id < problemCount; id++) {
            jobs.add(solverManager.solve(id, PlannerTestUtils.generateTestdataSolution(String.format("s%d", id))));
        }
        assertInitializedJobs(jobs);
    }

    private void assertSolveWithConsumer(
            int problemCount, SolverManager<TestdataSolution, Long> solverManager, boolean listenWhileSolving)
            throws ExecutionException, InterruptedException {

        // Two solutions should be created for every problem.
        Map<Long, List<TestdataSolution>> solutionMap = new HashMap<>(problemCount * 2);

        CountDownLatch finalBestSolutionConsumed = new CountDownLatch(problemCount);
        List<SolverJob<TestdataSolution, Long>> jobs = new ArrayList<>(problemCount);

        for (long id = 0; id < problemCount; id++) {
            List<TestdataSolution> consumedBestSolutions = Collections.synchronizedList(new ArrayList<>());
            String solutionName = String.format("s%d", id);
            if (listenWhileSolving) {
                jobs.add(solverManager.solveBuilder()
                        .withProblemId(id)
                        .withProblemFinder(problemId -> PlannerTestUtils.generateTestdataSolution(solutionName, 2))
                        .withBestSolutionConsumer(consumedBestSolutions::add)
                        .withFinalBestSolutionConsumer((finalBestSolution) -> {
                            finalBestSolutionConsumed.countDown();
                        })
                        .run());
            } else {
                jobs.add(solverManager.solveBuilder()
                        .withProblemId(id)
                        .withProblemFinder(problemId -> PlannerTestUtils.generateTestdataSolution(solutionName, 2))
                        .withFinalBestSolutionConsumer((finalBestSolution) -> {
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
        for (List<TestdataSolution> consumedSolution : consumedSolutions.values()) {
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
        TestdataEntity entity = solution.getEntityList().get(0);
        assertThat(entity.getCode()).isEqualTo("e1");
        assertThat(entity.getValue().getCode()).isEqualTo("v1");
        entity = solution.getEntityList().get(1);
        assertThat(entity.getCode()).isEqualTo("e2");
        assertThat(entity.getValue().getCode()).isEqualTo("v2");
    }

    private void assertConsumedFirstBestSolution(TestdataSolution solution) {
        TestdataEntity entity = solution.getEntityList().get(0);
        assertThat(entity.getCode()).isEqualTo("e1");
        assertThat(entity.getValue().getCode()).isEqualTo("v1");
        entity = solution.getEntityList().get(1);
        assertThat(entity.getCode()).isEqualTo("e2");
        assertThat(entity.getValue()).isNull();
    }

    @Test
    @Timeout(60)
    void runSameIdProcesses_throwsIllegalStateException() {
        SolverManagerConfig solverManagerConfig = new SolverManagerConfig();

        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class,
                TestdataEntity.class)
                .withPhases(createPhaseWithConcurrentSolvingStart(2));

        solverManager = SolverManager.create(solverConfig, solverManagerConfig);

        solverManager.solve(1L, PlannerTestUtils.generateTestdataSolution("s1"));
        assertThatThrownBy(() -> solverManager.solve(1L, PlannerTestUtils.generateTestdataSolution("s1")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("already solving");
    }

    @Test
    @Timeout(60)
    void addProblemChange() throws InterruptedException, ExecutionException {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        solverConfig.setDaemon(true);
        solverManager = SolverManager.create(solverConfig);
        final long problemId = 1L;

        final int entityAndValueCount = 4;
        AtomicReference<TestdataSolution> bestSolution = new AtomicReference<>();
        solverManager.solveBuilder()
                .withProblemId(problemId)
                .withProblemFinder(id -> PlannerTestUtils.generateTestdataSolution("s1", entityAndValueCount))
                .withBestSolutionConsumer(bestSolution::set)
                .run();

        CompletableFuture<Void> futureChange = solverManager
                .addProblemChange(problemId, (workingSolution, problemChangeDirector) -> {
                    problemChangeDirector.addProblemFact(new TestdataValue("addedValue"),
                            workingSolution.getValueList()::add);
                });

        futureChange.get();
        assertThat(futureChange).isCompleted();
        assertThat(bestSolution.get().getValueList()).hasSize(entityAndValueCount + 1);
    }

    @Test
    @Timeout(60)
    void addProblemChangeToNonExistingProblem_failsFast() {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        solverManager = SolverManager.create(solverConfig);

        solverManager.solveBuilder()
                .withProblemId(1L)
                .withProblemFinder(id -> PlannerTestUtils.generateTestdataSolution("s1", 4))
                .withBestSolutionConsumer(testdataSolution -> {
                })
                .run();

        final long nonExistingProblemId = 999L;
        assertThatIllegalStateException()
                .isThrownBy(() -> solverManager.addProblemChange(nonExistingProblemId,
                        (workingSolution, problemChangeDirector) -> problemChangeDirector.addProblemFact(
                                new TestdataValue("addedValue"),
                                workingSolution.getValueList()::add)))
                .withMessageContaining(String.valueOf(nonExistingProblemId));
    }

    @Test
    @Timeout(60)
    void addProblemChangeToWaitingSolver() throws InterruptedException, ExecutionException {
        CountDownLatch solvingPausedLatch = new CountDownLatch(1);
        PhaseConfig<?> pausedPhaseConfig = new CustomPhaseConfig().withCustomPhaseCommands(
                scoreDirector -> {
                    try {
                        solvingPausedLatch.await();
                    } catch (InterruptedException e) {
                        fail("CountDownLatch failed.");
                    }
                });

        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(pausedPhaseConfig, new ConstructionHeuristicPhaseConfig());
        // Allow only a single active solver.
        SolverManagerConfig solverManagerConfig = new SolverManagerConfig().withParallelSolverCount("1");
        solverManager = SolverManager.create(solverConfig, solverManagerConfig);

        // The first solver waits until the test sends a problem change.
        solverManager.solve(1L, PlannerTestUtils.generateTestdataSolution("s1", 4));

        // The second solver is scheduled and waits for the fist solver to finish.
        final long secondProblemId = 2L;
        final int entityAndValueCount = 4;
        AtomicReference<TestdataSolution> bestSolution = new AtomicReference<>();
        solverManager.solveBuilder()
                .withProblemId(secondProblemId)
                .withProblemFinder(id -> PlannerTestUtils.generateTestdataSolution("s2", entityAndValueCount))
                .withBestSolutionConsumer(bestSolution::set)
                .run();

        CompletableFuture<Void> futureChange = solverManager
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

    @Test
    @Timeout(60)
    void terminateSolverJobEarly_stillReturnsBestSolution() throws ExecutionException, InterruptedException {
        SolverManagerConfig solverManagerConfig = new SolverManagerConfig();
        CountDownLatch solvingStartedLatch = new CountDownLatch(1);
        PhaseConfig<?> pausedPhaseConfig = new CustomPhaseConfig().withCustomPhaseCommands(
                scoreDirector -> solvingStartedLatch.countDown());

        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(pausedPhaseConfig, new ConstructionHeuristicPhaseConfig());
        solverManager = SolverManager.create(solverConfig, solverManagerConfig);

        SolverJob<TestdataSolution, Long> solverJob =
                solverManager.solve(1L, PlannerTestUtils.generateTestdataSolution("s1"));
        solvingStartedLatch.await();
        solverJob.terminateEarly();
        TestdataSolution result = solverJob.getFinalBestSolution();
        assertThat(result).isNotNull();
        assertThat(solverJob.isTerminatedEarly()).isTrue();
    }

    @Test
    @Timeout(60)
    void terminateScheduledSolverJobEarly_returnsInputProblem() throws ExecutionException, InterruptedException {
        CountDownLatch solvingPausedLatch = new CountDownLatch(1);
        PhaseConfig<?> pausedPhaseConfig = new CustomPhaseConfig().withCustomPhaseCommands(
                scoreDirector -> {
                    try {
                        solvingPausedLatch.await();
                    } catch (InterruptedException e) {
                        fail("CountDownLatch failed.");
                    }
                });

        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(pausedPhaseConfig, new ConstructionHeuristicPhaseConfig());
        // Allow only a single active solver.
        SolverManagerConfig solverManagerConfig = new SolverManagerConfig().withParallelSolverCount("1");
        solverManager = SolverManager.create(solverConfig, solverManagerConfig);

        // The first solver waits.
        solverManager.solve(1L, PlannerTestUtils.generateTestdataSolution("s1", 4));
        TestdataSolution inputProblem = PlannerTestUtils.generateTestdataSolution("s2", 4);
        SolverJob<TestdataSolution, Long> solverJob = solverManager.solve(2L, inputProblem);

        solverJob.terminateEarly();
        TestdataSolution result = solverJob.getFinalBestSolution();
        assertThat(result).isSameAs(inputProblem);
        assertThat(solverJob.isTerminatedEarly()).isTrue();
    }

    public static class CustomThreadFactory implements ThreadFactory {
        private static final String CUSTOM_THREAD_NAME = "CustomThread";

        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, CUSTOM_THREAD_NAME);
        }
    }

    @Test
    @Timeout(60)
    void threadFactoryIsUsed() throws ExecutionException, InterruptedException {
        var threadCheckingPhaseConfig = new CustomPhaseConfig().withCustomPhaseCommands(
                scoreDirector -> {
                    if (!Thread.currentThread().getName().equals(CustomThreadFactory.CUSTOM_THREAD_NAME)) {
                        fail("Custom thread factory not used");
                    }
                });

        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(threadCheckingPhaseConfig, new ConstructionHeuristicPhaseConfig());

        var solverManagerConfig = new SolverManagerConfig().withThreadFactoryClass(CustomThreadFactory.class);
        solverManager = SolverManager.create(solverConfig, solverManagerConfig);

        var inputProblem = PlannerTestUtils.generateTestdataSolution("s1", 4);
        var solverJob = solverManager.solve(1L, inputProblem);

        TestdataSolution result = solverJob.getFinalBestSolution();
        assertThat(result).isNotNull();
    }
}
