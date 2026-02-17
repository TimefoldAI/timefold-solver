package ai.timefold.solver.core.impl.solver;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import ai.timefold.solver.core.api.solver.phase.PhaseCommandContext;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicType;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchPhaseConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchType;
import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.monitoring.MonitoringConfig;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedChangeMove;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.score.TestdataHardSoftScoreSolution;
import ai.timefold.solver.core.testutil.AbstractMeterTest;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

@ExtendWith(SoftAssertionsExtension.class)
class SolverMetricsIT extends AbstractMeterTest {

    @Test
    void checkDefaultMeters() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class);
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);

        var solver = (DefaultSolver<TestdataSolution>) solverFactory.buildSolver();
        meterRegistry.publish();
        assertThat(meterRegistry.getMeters().stream().map(Meter::getId)).isEmpty();

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        var updatedTime = new AtomicBoolean();
        var latch = new CountDownLatch(1);
        solver.addEventListener(event -> {
            if (!updatedTime.get()) {
                assertThat(meterRegistry.getMeters().stream().map(Meter::getId))
                        .containsExactlyInAnyOrder(
                                new Meter.Id(SolverMetric.SOLVE_DURATION.getMeterId(),
                                        Tags.empty(),
                                        null,
                                        null,
                                        Meter.Type.LONG_TASK_TIMER),
                                new Meter.Id(SolverMetric.ERROR_COUNT.getMeterId(),
                                        Tags.empty(),
                                        null,
                                        null,
                                        Meter.Type.COUNTER),
                                new Meter.Id(SolverMetric.SCORE_CALCULATION_COUNT.getMeterId(),
                                        Tags.empty(),
                                        null,
                                        null,
                                        Meter.Type.GAUGE),
                                new Meter.Id(SolverMetric.MOVE_EVALUATION_COUNT.getMeterId(),
                                        Tags.empty(),
                                        null,
                                        null,
                                        Meter.Type.GAUGE),
                                new Meter.Id(SolverMetric.PROBLEM_ENTITY_COUNT.getMeterId(),
                                        Tags.empty(),
                                        null,
                                        null,
                                        Meter.Type.GAUGE),
                                new Meter.Id(SolverMetric.PROBLEM_VARIABLE_COUNT.getMeterId(),
                                        Tags.empty(),
                                        null,
                                        null,
                                        Meter.Type.GAUGE),
                                new Meter.Id(SolverMetric.PROBLEM_VALUE_COUNT.getMeterId(),
                                        Tags.empty(),
                                        null,
                                        null,
                                        Meter.Type.GAUGE),
                                new Meter.Id(SolverMetric.PROBLEM_SIZE_LOG.getMeterId(),
                                        Tags.empty(),
                                        null,
                                        null,
                                        Meter.Type.GAUGE));
                updatedTime.set(true);
            }
            latch.countDown();
        });
        solver.solve(solution);

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assertions.fail("Failed waiting for the event to happen.", e);
        }

        // Score calculation and problem scale counts should be removed
        // since registering multiple gauges with the same id
        // make it return the average, and the solver holds
        // onto the solver scope, meaning it won't automatically
        // be deregistered.
        assertThat(meterRegistry.getMeters().stream().map(Meter::getId))
                .containsExactlyInAnyOrder(
                        new Meter.Id(SolverMetric.SOLVE_DURATION.getMeterId(),
                                Tags.empty(),
                                null,
                                null,
                                Meter.Type.LONG_TASK_TIMER),
                        new Meter.Id(SolverMetric.ERROR_COUNT.getMeterId(),
                                Tags.empty(),
                                null,
                                null,
                                Meter.Type.COUNTER));
    }

    @Test
    void checkDefaultMetersTags() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);

        var solver = (DefaultSolver<TestdataSolution>) solverFactory.buildSolver();
        solver.setMonitorTagMap(Map.of("tag.key", "tag.value"));
        meterRegistry.publish();
        assertThat(meterRegistry.getMeters().stream().map(Meter::getId)).isEmpty();

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        var updatedTime = new AtomicBoolean();
        var latch = new CountDownLatch(1);
        solver.addEventListener(event -> {
            if (!updatedTime.get()) {
                assertThat(meterRegistry.getMeters().stream().map(Meter::getId))
                        .containsExactlyInAnyOrder(
                                new Meter.Id(SolverMetric.SOLVE_DURATION.getMeterId(),
                                        Tags.empty(),
                                        null,
                                        null,
                                        Meter.Type.LONG_TASK_TIMER),
                                new Meter.Id(SolverMetric.ERROR_COUNT.getMeterId(),
                                        Tags.empty(),
                                        null,
                                        null,
                                        Meter.Type.COUNTER),
                                new Meter.Id(SolverMetric.SCORE_CALCULATION_COUNT.getMeterId(),
                                        Tags.of("tag.key", "tag.value"),
                                        null,
                                        null,
                                        Meter.Type.GAUGE),
                                new Meter.Id(SolverMetric.MOVE_EVALUATION_COUNT.getMeterId(),
                                        Tags.of("tag.key", "tag.value"),
                                        null,
                                        null,
                                        Meter.Type.GAUGE),
                                new Meter.Id(SolverMetric.PROBLEM_ENTITY_COUNT.getMeterId(),
                                        Tags.of("tag.key", "tag.value"),
                                        null,
                                        null,
                                        Meter.Type.GAUGE),
                                new Meter.Id(SolverMetric.PROBLEM_VARIABLE_COUNT.getMeterId(),
                                        Tags.of("tag.key", "tag.value"),
                                        null,
                                        null,
                                        Meter.Type.GAUGE),
                                new Meter.Id(SolverMetric.PROBLEM_VALUE_COUNT.getMeterId(),
                                        Tags.of("tag.key", "tag.value"),
                                        null,
                                        null,
                                        Meter.Type.GAUGE),
                                new Meter.Id(SolverMetric.PROBLEM_SIZE_LOG.getMeterId(),
                                        Tags.of("tag.key", "tag.value"),
                                        null,
                                        null,
                                        Meter.Type.GAUGE));
                updatedTime.set(true);
            }
            latch.countDown();
        });
        solver.solve(solution);

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assertions.fail("Failed waiting for the event to happen.", e);
        }

        // Score calculation and problem scale counts should be removed
        // since registering multiple gauges with the same id
        // make it return the average, and the solver holds
        // onto the solver scope, meaning it won't automatically
        // be deregistered.
        assertThat(meterRegistry.getMeters().stream().map(Meter::getId))
                .containsExactlyInAnyOrder(
                        new Meter.Id(SolverMetric.SOLVE_DURATION.getMeterId(),
                                Tags.empty(),
                                null,
                                null,
                                Meter.Type.LONG_TASK_TIMER),
                        new Meter.Id(SolverMetric.ERROR_COUNT.getMeterId(),
                                Tags.empty(),
                                null,
                                null,
                                Meter.Type.COUNTER));
    }

    @Test
    void solveMetrics() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class);
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);

        var solver = solverFactory.buildSolver();
        ((DefaultSolver<TestdataSolution>) solver).setMonitorTagMap(Map.of("solver.id", UUID.randomUUID().toString()));
        meterRegistry.publish();

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        var latch = new CountDownLatch(1);
        var updatedTime = new AtomicBoolean();
        solver.addEventListener(event -> {
            if (!updatedTime.get()) {
                meterRegistry.getClock().addSeconds(2);
                meterRegistry.publish();
                assertThat(meterRegistry.getMeasurement(SolverMetric.SOLVE_DURATION.getMeterId(), "ACTIVE_TASKS")).isOne();
                assertThat(meterRegistry.getMeasurement(SolverMetric.SOLVE_DURATION.getMeterId(), "DURATION").longValue())
                        .isEqualTo(2L);

                // 2 Entities
                assertThat(meterRegistry.getMeasurement(SolverMetric.PROBLEM_ENTITY_COUNT.getMeterId(), "VALUE").longValue())
                        .isEqualTo(2L);
                // 1 Genuine variable on 2 entities = 2 total variables
                assertThat(meterRegistry.getMeasurement(SolverMetric.PROBLEM_VARIABLE_COUNT.getMeterId(), "VALUE").longValue())
                        .isEqualTo(2L);
                // The maximum assignable value count of any variable is 2
                assertThat(meterRegistry.getMeasurement(SolverMetric.PROBLEM_VALUE_COUNT.getMeterId(), "VALUE").longValue())
                        .isEqualTo(2L);
                updatedTime.set(true);
            }
            latch.countDown();
        });
        solution = solver.solve(solution);

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assertions.fail("Failed waiting for the event to happen.", e);
        }
        meterRegistry.publish();
        assertThat(solution).isNotNull();
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getValue() == null)).isEmpty();

        assertThat(meterRegistry.getMeasurement(SolverMetric.SOLVE_DURATION.getMeterId(), "DURATION")).isZero();
        assertThat(meterRegistry.getMeasurement(SolverMetric.SOLVE_DURATION.getMeterId(), "ACTIVE_TASKS")).isZero();
        assertThat(meterRegistry.getMeasurement(SolverMetric.ERROR_COUNT.getMeterId(), "COUNT")).isZero();
        assertThat(meterRegistry.getMeasurement(SolverMetric.SCORE_CALCULATION_COUNT.getMeterId(), "VALUE")).isPositive();
        assertThat(meterRegistry.getMeasurement(SolverMetric.MOVE_EVALUATION_COUNT.getMeterId(), "VALUE")).isPositive();
    }

    @Test
    void solveMetricsProblemChange() throws InterruptedException, ExecutionException {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class);
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);

        var solver = solverFactory.buildSolver();
        meterRegistry.publish();

        final var solution = new TestdataSolution("s1");
        solution.setValueList(new ArrayList<>(List.of(new TestdataValue("v1"), new TestdataValue("v2"))));
        solution.setEntityList(new ArrayList<>(List.of(new TestdataEntity("e1"), new TestdataEntity("e2"))));

        var latch = new CountDownLatch(1);
        solver.addEventListener(bestSolutionChangedEvent -> {
            try {
                latch.await();
                meterRegistry.publish();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        var executorService = Executors.newSingleThreadExecutor();
        var job = executorService.submit(() -> {
            solver.solve(solution);
        });

        solver.addProblemChange((workingSolution, problemChangeDirector) -> {
            problemChangeDirector.addEntity(new TestdataEntity("added entity"), workingSolution.getEntityList()::add);
            problemChangeDirector.addProblemFact(new TestdataValue("added value"), workingSolution.getValueList()::add);
        });

        latch.countDown();
        job.get();
        // 3 Entities
        assertThat(
                meterRegistry.getMeasurement(SolverMetric.PROBLEM_ENTITY_COUNT.getMeterId(), "VALUE").longValue())
                .isEqualTo(3L);
        // 1 Genuine variable on 3 entities = 3 total variables
        assertThat(
                meterRegistry.getMeasurement(SolverMetric.PROBLEM_VARIABLE_COUNT.getMeterId(), "VALUE").longValue())
                .isEqualTo(3L);
        assertThat(meterRegistry.getMeasurement(SolverMetric.PROBLEM_VALUE_COUNT.getMeterId(), "VALUE").longValue())
                .isEqualTo(3L);
    }

    public static class BestScoreMetricEasyScoreCalculator
            implements EasyScoreCalculator<TestdataHardSoftScoreSolution, HardSoftScore> {

        @Override
        public @NonNull HardSoftScore calculateScore(@NonNull TestdataHardSoftScoreSolution testdataSolution) {
            var count = testdataSolution.getEntityList()
                    .stream()
                    .filter(e -> e.getValue() != null)
                    .filter(e -> e.getValue().getCode().startsWith("reward"))
                    .count();
            return HardSoftScore.ofSoft((int) count);
        }
    }

    public static class NoneValueSelectionFilter
            implements SelectionFilter<TestdataHardSoftScoreSolution, SelectorBasedChangeMove<TestdataHardSoftScoreSolution>> {
        @Override
        public boolean accept(ScoreDirector<TestdataHardSoftScoreSolution> scoreDirector,
                SelectorBasedChangeMove<TestdataHardSoftScoreSolution> selection) {
            return ((TestdataValue) (selection.getToPlanningValue())).getCode().equals("none");
        }
    }

    @Test
    void solveBestScoreMetrics() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataHardSoftScoreSolution.class, TestdataEntity.class);
        solverConfig.setScoreDirectorFactoryConfig(
                new ScoreDirectorFactoryConfig().withEasyScoreCalculatorClass(BestScoreMetricEasyScoreCalculator.class));
        solverConfig.setTerminationConfig(new TerminationConfig().withBestScoreLimit("0hard/2soft"));
        solverConfig.setMonitoringConfig(new MonitoringConfig()
                .withSolverMetricList(List.of(SolverMetric.BEST_SCORE)));
        solverConfig.setPhaseConfigList(List.of(
                // Force Timefold to select "none" value which reward 0 soft
                new ConstructionHeuristicPhaseConfig()
                        .withConstructionHeuristicType(ConstructionHeuristicType.FIRST_FIT)
                        .withMoveSelectorConfigList(
                                List.of(new ChangeMoveSelectorConfig()
                                        .withFilterClass(NoneValueSelectionFilter.class))),
                // Then do a local search, which allow Timefold to select "reward" value
                // which reward 1 soft per entity
                new LocalSearchPhaseConfig()
                        .withLocalSearchType(LocalSearchType.HILL_CLIMBING)));
        SolverFactory<TestdataHardSoftScoreSolution> solverFactory = SolverFactory.create(solverConfig);

        var solver = solverFactory.buildSolver();
        ((DefaultSolver<TestdataHardSoftScoreSolution>) solver)
                .setMonitorTagMap(Map.of("solver.id", UUID.randomUUID().toString()));
        meterRegistry.publish();
        var solution = new TestdataHardSoftScoreSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("none"), new TestdataValue("reward")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));
        var step = new AtomicInteger(-1);

        var latch = new CountDownLatch(1);
        solver.addEventListener(event -> {
            meterRegistry.publish();

            // This event listener is added before the best score event listener
            // so it is one step behind
            if (step.get() != -1) {
                assertThat(
                        meterRegistry.getMeasurement(SolverMetric.BEST_SCORE.getMeterId() + ".hard.score", "VALUE").intValue())
                        .isZero();
            }
            if (step.get() == 0) {
                assertThat(
                        meterRegistry.getMeasurement(SolverMetric.BEST_SCORE.getMeterId() + ".soft.score", "VALUE").intValue())
                        .isZero();
            } else if (step.get() == 1) {
                assertThat(
                        meterRegistry.getMeasurement(SolverMetric.BEST_SCORE.getMeterId() + ".soft.score", "VALUE").intValue())
                        .isEqualTo(1);
            } else if (step.get() == 2) {
                assertThat(
                        meterRegistry.getMeasurement(SolverMetric.BEST_SCORE.getMeterId() + ".soft.score", "VALUE").intValue())
                        .isEqualTo(2);
            }
            step.incrementAndGet();
            latch.countDown();
        });
        solution = solver.solve(solution);

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Failed waiting for the event to happen.", e);
        }
        assertThat(step.get()).isEqualTo(2);
        meterRegistry.publish();
        assertThat(solution).isNotNull();
        assertThat(meterRegistry.getMeasurement(SolverMetric.BEST_SCORE.getMeterId() + ".hard.score", "VALUE").intValue())
                .isZero();
        assertThat(meterRegistry.getMeasurement(SolverMetric.BEST_SCORE.getMeterId() + ".soft.score", "VALUE").intValue())
                .isEqualTo(2);
    }

    private record SetTestdataEntityValueCustomPhaseCommand(TestdataEntity entity, TestdataValue value)
            implements
                PhaseCommand<TestdataHardSoftScoreSolution> {

        @Override
        public void changeWorkingSolution(PhaseCommandContext<TestdataHardSoftScoreSolution> context) {
            var workingEntity = context.lookupWorkingObject(entity);
            var workingValue = context.lookupWorkingObject(value);

            var move = Moves.change(context.getSolutionMetaModel()
                    .genuineEntity(TestdataEntity.class)
                    .basicVariable("value", TestdataValue.class), workingEntity, workingValue);
            context.execute(move);
        }
    }

    @Test
    void solveStepScoreMetrics() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataHardSoftScoreSolution.class, TestdataEntity.class);
        solverConfig.setScoreDirectorFactoryConfig(
                new ScoreDirectorFactoryConfig().withEasyScoreCalculatorClass(BestScoreMetricEasyScoreCalculator.class));
        solverConfig.setTerminationConfig(new TerminationConfig().withBestScoreLimit("0hard/3soft"));
        solverConfig.setMonitoringConfig(new MonitoringConfig()
                .withSolverMetricList(List.of(SolverMetric.STEP_SCORE)));

        var solution = new TestdataHardSoftScoreSolution("s1");
        var e1 = new TestdataEntity("e1");
        var e2 = new TestdataEntity("e2");
        var e3 = new TestdataEntity("e3");
        var none = new TestdataValue("none");
        var reward = new TestdataValue("reward");
        solution.setValueList(Arrays.asList(none, reward));
        solution.setEntityList(Arrays.asList(e1, e2, e3));

        solverConfig.setPhaseConfigList(List.of(
                // Force Timefold to select "none" value which reward 0 soft
                new ConstructionHeuristicPhaseConfig()
                        .withConstructionHeuristicType(ConstructionHeuristicType.FIRST_FIT)
                        .withMoveSelectorConfigList(
                                List.of(new ChangeMoveSelectorConfig()
                                        .withFilterClass(NoneValueSelectionFilter.class))),
                // Then do a custom phase, to force certain steps to be taken
                new CustomPhaseConfig()
                        .withCustomPhaseCommands(
                                new SetTestdataEntityValueCustomPhaseCommand(e1, reward),
                                new SetTestdataEntityValueCustomPhaseCommand(e2, reward),
                                new SetTestdataEntityValueCustomPhaseCommand(e1, none),
                                new SetTestdataEntityValueCustomPhaseCommand(e1, reward),
                                new SetTestdataEntityValueCustomPhaseCommand(e3, reward))));
        SolverFactory<TestdataHardSoftScoreSolution> solverFactory = SolverFactory.create(solverConfig);

        var solver = solverFactory.buildSolver();
        ((DefaultSolver<TestdataHardSoftScoreSolution>) solver)
                .setMonitorTagMap(Map.of("solver.id", UUID.randomUUID().toString()));
        var step = new AtomicInteger(-1);

        ((DefaultSolver<TestdataHardSoftScoreSolution>) solver)
                .addPhaseLifecycleListener(new PhaseLifecycleListenerAdapter<TestdataHardSoftScoreSolution>() {
                    @Override
                    public void stepEnded(AbstractStepScope<TestdataHardSoftScoreSolution> stepScope) {
                        super.stepEnded(stepScope);
                        meterRegistry.publish();

                        // first 3 steps are construction heuristic steps and don't have a step score since it uninitialized
                        if (step.get() < 2) {
                            step.incrementAndGet();
                            return;
                        }

                        assertThat(
                                meterRegistry.getMeasurement(SolverMetric.STEP_SCORE.getMeterId() + ".hard.score", "VALUE")
                                        .intValue())
                                .isZero();

                        if (step.get() == 2) {
                            assertThat(
                                    meterRegistry.getMeasurement(SolverMetric.STEP_SCORE.getMeterId() + ".soft.score", "VALUE")
                                            .intValue())
                                    .isZero();
                        } else if (step.get() == 3) {
                            assertThat(
                                    meterRegistry.getMeasurement(SolverMetric.STEP_SCORE.getMeterId() + ".soft.score", "VALUE")
                                            .intValue())
                                    .isEqualTo(1);
                        } else if (step.get() == 4) {
                            assertThat(
                                    meterRegistry.getMeasurement(SolverMetric.STEP_SCORE.getMeterId() + ".soft.score", "VALUE")
                                            .intValue())
                                    .isEqualTo(2);
                        } else if (step.get() == 5) {
                            assertThat(
                                    meterRegistry.getMeasurement(SolverMetric.STEP_SCORE.getMeterId() + ".soft.score", "VALUE")
                                            .intValue())
                                    .isEqualTo(1);
                        } else if (step.get() == 6) {
                            assertThat(
                                    meterRegistry.getMeasurement(SolverMetric.STEP_SCORE.getMeterId() + ".soft.score", "VALUE")
                                            .intValue())
                                    .isEqualTo(2);
                        }
                        step.incrementAndGet();
                    }
                });
        solution = solver.solve(solution);

        assertThat(step.get()).isEqualTo(7);
        meterRegistry.publish();
        assertThat(solution).isNotNull();
        assertThat(meterRegistry.getMeasurement(SolverMetric.STEP_SCORE.getMeterId() + ".hard.score", "VALUE").intValue())
                .isZero();
        assertThat(meterRegistry.getMeasurement(SolverMetric.STEP_SCORE.getMeterId() + ".soft.score", "VALUE").intValue())
                .isEqualTo(3);
    }

    public static class ErrorThrowingEasyScoreCalculator implements EasyScoreCalculator<TestdataSolution, SimpleScore> {

        @Override
        public @NonNull SimpleScore calculateScore(@NonNull TestdataSolution testdataSolution) {
            throw new IllegalStateException("Thrown exception in constraint provider");
        }
    }

    @Test
    void solveMetricsError() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withScoreDirectorFactory(
                        new ScoreDirectorFactoryConfig().withEasyScoreCalculatorClass(ErrorThrowingEasyScoreCalculator.class));
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);

        var solver = solverFactory.buildSolver();
        ((DefaultSolver<TestdataSolution>) solver).setMonitorTagMap(Map.of("solver.id", UUID.randomUUID().toString()));
        meterRegistry.publish();

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        meterRegistry.publish();

        assertThatCode(() -> solver.solve(solution))
                .hasStackTraceContaining("Thrown exception in constraint provider");

        meterRegistry.getClock().addSeconds(1);
        meterRegistry.publish();
        assertThat(meterRegistry.getMeasurement(SolverMetric.SOLVE_DURATION.getMeterId(), "ACTIVE_TASKS")).isZero();
        assertThat(meterRegistry.getMeasurement(SolverMetric.SOLVE_DURATION.getMeterId(), "DURATION")).isZero();
        assertThat(meterRegistry.getMeasurement(SolverMetric.ERROR_COUNT.getMeterId(), "COUNT")).isOne();
    }

    @Test
    void chWithCustomMetrics() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig())
                .withMonitoringConfig(new MonitoringConfig().withSolverMetricList(List.of(SolverMetric.MOVE_COUNT_PER_TYPE)));

        var problem = new TestdataSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        problem.setValueList(Arrays.asList(v1, v2, v3));
        problem.setEntityList(Arrays.asList(
                new TestdataEntity("e1", null),
                new TestdataEntity("e2", v2),
                new TestdataEntity("e3", v1)));

        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<TestdataSolution> solver = solverFactory.buildSolver();
        var moveCountPerChange = new AtomicLong();
        ((DefaultSolver<TestdataSolution>) solver).addPhaseLifecycleListener(new PhaseLifecycleListenerAdapter<>() {
            @Override
            public void solvingEnded(SolverScope<TestdataSolution> solverScope) {
                meterRegistry.publish();
                var changeMoveKey = "ChangeMove(TestdataEntity.value)";
                if (solverScope.getMoveCountTypes().contains(changeMoveKey)) {
                    var counter = meterRegistry
                            .getMeasurement(SolverMetric.MOVE_COUNT_PER_TYPE.getMeterId() + "." + changeMoveKey, "VALUE");
                    moveCountPerChange.set(counter.longValue());
                }
            }
        });
        solver.solve(problem);
        assertThat(moveCountPerChange.get()).isPositive();
    }

    @Test
    void chWithListVariableAndCustomMetrics() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class)
                .withPhases(new ConstructionHeuristicPhaseConfig())
                .withMonitoringConfig(new MonitoringConfig().withSolverMetricList(List.of(SolverMetric.MOVE_COUNT_PER_TYPE)));

        var problem = new TestdataListSolution();
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");
        problem.setValueList(Arrays.asList(v1, v2, v3));
        problem.setEntityList(List.of(new TestdataListEntity("e1", new ArrayList<>())));

        SolverFactory<TestdataListSolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<TestdataListSolution> solver = solverFactory.buildSolver();
        var moveCount = new AtomicLong();
        ((DefaultSolver<TestdataListSolution>) solver).addPhaseLifecycleListener(new PhaseLifecycleListenerAdapter<>() {
            @Override
            public void solvingEnded(SolverScope<TestdataListSolution> solverScope) {
                meterRegistry.publish();
                var changeMoveKey = "ListAssignMove(TestdataListEntity.valueList)";
                if (solverScope.getMoveCountTypes().contains(changeMoveKey)) {
                    var counter = meterRegistry
                            .getMeasurement(SolverMetric.MOVE_COUNT_PER_TYPE.getMeterId() + "." + changeMoveKey, "VALUE");
                    moveCount.set(counter.longValue());
                }
            }
        });
        solver.solve(problem);
        assertThat(moveCount.get()).isPositive();
    }

    @Test
    void esWithInitializedEntitiesAndMetric() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class,
                TestdataEntity.class);
        solverConfig.setPhaseConfigList(Collections.singletonList(new ExhaustiveSearchPhaseConfig()));

        var problem = new TestdataSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        problem.setValueList(Arrays.asList(v1, v2, v3));
        problem.setEntityList(Arrays.asList(
                new TestdataEntity("e1", null),
                new TestdataEntity("e2", v2),
                new TestdataEntity("e3", v1)));

        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<TestdataSolution> solver = solverFactory.buildSolver();
        AtomicReference<TestdataSolution> eventBestSolutionRef = new AtomicReference<>();
        solver.addEventListener(event -> eventBestSolutionRef.set(event.getNewBestSolution()));
        TestdataSolution solution = solver.solve(problem);
        assertThat(eventBestSolutionRef).doesNotHaveNullValue();

        assertThat(solution).isNotNull();
        var solvedE1 = solution.getEntityList().get(0);
        assertCode("e1", solvedE1);
        assertThat(solvedE1.getValue()).isNotNull();
        var solvedE2 = solution.getEntityList().get(1);
        assertCode("e2", solvedE2);
        assertThat(solvedE2.getValue()).isEqualTo(v2);
        var solvedE3 = solution.getEntityList().get(2);
        assertCode("e3", solvedE3);
        assertThat(solvedE3.getValue()).isEqualTo(v1);

        SolverMetric.MOVE_EVALUATION_COUNT.register(solver);
        SolverMetric.SCORE_CALCULATION_COUNT.register(solver);
        meterRegistry.publish();
        var scoreCount = meterRegistry.getMeasurement(SolverMetric.SCORE_CALCULATION_COUNT.getMeterId(), "VALUE");
        var moveCount = meterRegistry.getMeasurement(SolverMetric.MOVE_EVALUATION_COUNT.getMeterId(), "VALUE");
        assertThat(scoreCount).isPositive();
        assertThat(moveCount).isPositive();
    }

    @Test
    void esCustomMetrics() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class,
                TestdataEntity.class);
        solverConfig.withPhases(new ExhaustiveSearchPhaseConfig())
                .withMonitoringConfig(new MonitoringConfig().withSolverMetricList(List.of(SolverMetric.MOVE_COUNT_PER_TYPE)));

        var problem = new TestdataSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        problem.setValueList(Arrays.asList(v1, v2, v3));
        problem.setEntityList(Arrays.asList(
                new TestdataEntity("e1", null),
                new TestdataEntity("e2", v2),
                new TestdataEntity("e3", v1)));

        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<TestdataSolution> solver = solverFactory.buildSolver();
        var moveCountPerChange = new AtomicLong();
        ((DefaultSolver<TestdataSolution>) solver).addPhaseLifecycleListener(new PhaseLifecycleListenerAdapter<>() {
            @Override
            public void solvingEnded(SolverScope<TestdataSolution> solverScope) {
                meterRegistry.publish();
                var changeMoveKey = "ChangeMove(TestdataEntity.value)";
                if (solverScope.getMoveCountTypes().contains(changeMoveKey)) {
                    var counter = meterRegistry
                            .getMeasurement(SolverMetric.MOVE_COUNT_PER_TYPE.getMeterId() + "." + changeMoveKey, "VALUE");
                    moveCountPerChange.set(counter.longValue());
                }
            }
        });
        solver.solve(problem);
        assertThat(moveCountPerChange.get()).isPositive();
    }

    @Test
    void lsWithCustomMetrics() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        var phaseConfig = new LocalSearchPhaseConfig();
        phaseConfig.setTerminationConfig(new TerminationConfig().withScoreCalculationCountLimit(10L));
        solverConfig.withPhases(phaseConfig)
                .withMonitoringConfig(new MonitoringConfig().withSolverMetricList(List.of(SolverMetric.MOVE_COUNT_PER_TYPE)));

        var problem = new TestdataSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        problem.setValueList(Arrays.asList(v1, v2, v3));
        problem.setEntityList(Arrays.asList(
                new TestdataEntity("e1", v3),
                new TestdataEntity("e2", v2),
                new TestdataEntity("e3", v1)));

        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<TestdataSolution> solver = solverFactory.buildSolver();
        var moveCountPerChange = new AtomicLong();
        var moveCountPerSwap = new AtomicLong();
        ((DefaultSolver<TestdataSolution>) solver).addPhaseLifecycleListener(new PhaseLifecycleListenerAdapter<>() {
            @Override
            public void solvingEnded(SolverScope<TestdataSolution> solverScope) {
                meterRegistry.publish();
                var changeMoveKey = "ChangeMove(TestdataEntity.value)";
                if (solverScope.getMoveCountTypes().contains(changeMoveKey)) {
                    var counter = meterRegistry
                            .getMeasurement(SolverMetric.MOVE_COUNT_PER_TYPE.getMeterId() + "." + changeMoveKey, "VALUE");
                    moveCountPerChange.set(counter.longValue());
                }
                var swapMoveKey = "SwapMove(TestdataEntity.value)";
                if (solverScope.getMoveCountTypes().contains(swapMoveKey)) {
                    var counter = meterRegistry
                            .getMeasurement(SolverMetric.MOVE_COUNT_PER_TYPE.getMeterId() + "." + swapMoveKey, "VALUE");
                    moveCountPerSwap.set(counter.longValue());
                }
            }
        });
        solver.solve(problem);
        assertThat(moveCountPerChange.get()).isPositive();
        assertThat(moveCountPerSwap.get()).isPositive();
    }

    @Test
    void lsWithListVariableAndCustomMetrics() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class);
        var phaseConfig = new LocalSearchPhaseConfig();
        phaseConfig.setTerminationConfig(new TerminationConfig().withScoreCalculationCountLimit(10L));
        solverConfig.withPhases(phaseConfig)
                .withMonitoringConfig(new MonitoringConfig().withSolverMetricList(List.of(SolverMetric.MOVE_COUNT_PER_TYPE)));

        var problem = new TestdataListSolution();
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");
        problem.setValueList(Arrays.asList(v1, v2, v3));
        problem.setEntityList(List.of(new TestdataListEntity("e1", v1, v2, v3)));

        SolverFactory<TestdataListSolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<TestdataListSolution> solver = solverFactory.buildSolver();
        var moveCountPerChange = new AtomicLong();
        var moveCountPerSwap = new AtomicLong();
        var moveCountPer2Opt = new AtomicLong();
        ((DefaultSolver<TestdataListSolution>) solver).addPhaseLifecycleListener(new PhaseLifecycleListenerAdapter<>() {
            @Override
            public void solvingEnded(SolverScope<TestdataListSolution> solverScope) {
                meterRegistry.publish();
                var changeMoveKey = "ListChangeMove(TestdataListEntity.valueList)";
                if (solverScope.getMoveCountTypes().contains(changeMoveKey)) {
                    var counter = meterRegistry
                            .getMeasurement(SolverMetric.MOVE_COUNT_PER_TYPE.getMeterId() + "." + changeMoveKey, "VALUE");
                    moveCountPerChange.set(counter.longValue());
                }
                var swapMoveKey = "ListSwapMove(TestdataListEntity.valueList)";
                if (solverScope.getMoveCountTypes().contains(swapMoveKey)) {
                    var counter = meterRegistry
                            .getMeasurement(SolverMetric.MOVE_COUNT_PER_TYPE.getMeterId() + "." + swapMoveKey, "VALUE");
                    moveCountPerSwap.set(counter.longValue());
                }
                var twoOptMoveKey = "2-Opt(TestdataListEntity.valueList)";
                if (solverScope.getMoveCountTypes().contains(twoOptMoveKey)) {
                    var counter = meterRegistry
                            .getMeasurement(SolverMetric.MOVE_COUNT_PER_TYPE.getMeterId() + "." + twoOptMoveKey, "VALUE");
                    moveCountPer2Opt.set(counter.longValue());
                }
            }
        });
        solver.solve(problem);
        assertThat(moveCountPerChange.get()).isPositive();
        assertThat(moveCountPerSwap.get()).isPositive();
        assertThat(moveCountPer2Opt.get()).isPositive();
    }

}
