package ai.timefold.solver.core.impl.solver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.DiminishedReturnsTerminationConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEasyScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultSolverTerminationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSolverTerminationTest.class);

    @Test
    void stepCountTerminationAtSolverLevel() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestdataEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig())
                .withTerminationConfig(new TerminationConfig()
                        .withStepCountLimit(1));
        var solution = TestdataSolution.generateSolution(2, 2);
        solution.getEntityList().forEach(entity -> entity.setValue(null)); // Uninitialize.
        var solver = SolverFactory.<TestdataSolution> create(solverConfig)
                .buildSolver();
        var bestSolution = solver.solve(solution);
        // 2 entities means 2 steps, but the step count limit is 1.
        // Therefore the best solution is uninitialized.
        Assertions.assertThat(bestSolution.getScore()).isEqualTo(SimpleScore.ofUninitialized(-1, 0));
    }

    @Test
    void stepCountTerminationAtPhaseLevel() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestdataEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig()
                        .withTerminationConfig(new TerminationConfig()
                                .withStepCountLimit(1)));
        var solution = TestdataSolution.generateSolution(2, 2);
        solution.getEntityList().forEach(entity -> entity.setValue(null)); // Uninitialize.
        var solver = SolverFactory.<TestdataSolution> create(solverConfig)
                .buildSolver();
        var bestSolution = solver.solve(solution);
        // 2 entities means 2 steps, but the step count limit is 1.
        // Therefore the best solution is uninitialized.
        Assertions.assertThat(bestSolution.getScore()).isEqualTo(SimpleScore.ofUninitialized(-1, 0));
    }

    @Test
    void diminishedReturnsTerminationAtSolverLevel() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestdataEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig())
                .withTerminationConfig(new TerminationConfig()
                        .withDiminishedReturnsConfig(new DiminishedReturnsTerminationConfig()));
        var solution = TestdataSolution.generateSolution(2, 2);
        solution.getEntityList().forEach(entity -> entity.setValue(null)); // Uninitialize.
        var solver = SolverFactory.<TestdataSolution> create(solverConfig)
                .buildSolver();
        var bestSolution = solver.solve(solution);
        Assertions.assertThat(bestSolution.getScore()).isEqualTo(SimpleScore.ZERO);
    }

    @Test
    void diminishedReturnsTerminationInapplicableAtPhaseLevel() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestdataEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig()
                        .withTerminationConfig(new TerminationConfig()
                                .withDiminishedReturnsConfig(new DiminishedReturnsTerminationConfig())));
        var solverFactory = SolverFactory.<TestdataSolution> create(solverConfig);
        Assertions.assertThatThrownBy(solverFactory::buildSolver)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("includes some terminations which are not applicable")
                .hasMessageContaining("DiminishedReturns");
    }

    @Test
    void unimprovedStepCountTerminationInapplicableAtPhaseLevel() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestdataEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig()
                        .withTerminationConfig(new TerminationConfig()
                                .withUnimprovedStepCountLimit(1)));
        var solverFactory = SolverFactory.<TestdataSolution> create(solverConfig);
        Assertions.assertThatThrownBy(solverFactory::buildSolver)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("includes some terminations which are not applicable")
                .hasMessageContaining("UnimprovedStepCount");
    }

    @Test
    void unimprovedTimeSpentTerminationInapplicableAtPhaseLevel() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestdataEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig()
                        .withTerminationConfig(new TerminationConfig()
                                .withUnimprovedMillisecondsSpentLimit(1L)));
        var solverFactory = SolverFactory.<TestdataSolution> create(solverConfig);
        Assertions.assertThatThrownBy(solverFactory::buildSolver)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("includes some terminations which are not applicable")
                .hasMessageContaining("UnimprovedTimeMillisSpent");
    }

    @Test
    @Timeout(10)
    void terminateEarlyConstructionHeuristic() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class)
                .withEasyScoreCalculatorClass(DummySimpleScoreThrowingEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());

        var solution = TestdataSolution.generateSolution(2, 2);
        solution.getEntityList().forEach(entity -> entity.setValue(null)); // Uninitialize.
        var solver = SolverFactory.<TestdataSolution> create(solverConfig)
                .buildSolver();
        DummySimpleScoreThrowingEasyScoreCalculator.SOLVER.set(solver);

        var resultingSolution = solver.solve(solution);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(resultingSolution).isNotNull();
            softly.assertThat(resultingSolution.getScore().initScore()).isLessThan(0);
        });
        Assertions.assertThat(solution).isNotNull();
    }

    @Test
    @Timeout(10)
    void terminateEarlyLocalSearch() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withConstraintProviderClass(TestdataConstraintProviderNonzeroValues.class)
                .withPhases(new LocalSearchPhaseConfig());

        var solution = TestdataSolution.generateSolution(2, 2);
        var solver = SolverFactory.<TestdataSolution> create(solverConfig)
                .buildSolver();
        var latch = new CountDownLatch(1);
        solver.addEventListener(event -> {
            solver.terminateEarly(); // Once the solver is running, terminate it.
            latch.countDown();
            LOGGER.info("Sent request to terminate early.");
        });

        var resultingSolution = solver.solve(solution);
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assertions.fail("Solver did not terminate early.");
        }
        Assertions.assertThat(resultingSolution).isNotNull();
    }

    @NullMarked
    public static final class DummySimpleScoreThrowingEasyScoreCalculator
            implements EasyScoreCalculator<TestdataSolution, SimpleScore> {

        public static final AtomicReference<@Nullable Solver<TestdataSolution>> SOLVER =
                new AtomicReference<>();

        public DummySimpleScoreThrowingEasyScoreCalculator() {
            SOLVER.set(null);
        }

        @Override
        public SimpleScore calculateScore(TestdataSolution solution) {
            var unassignedCount = (int) solution.getEntityList().stream()
                    .filter(entity -> entity.getValue() == null)
                    .count();
            return switch (unassignedCount) {
                case 2 -> { // CH is starting, ensure we are in the right state.
                    Assumptions.assumeTrue(SOLVER.get() != null);
                    yield SimpleScore.ZERO;
                }
                case 1 -> { // CH did one step, guaranteed running.
                    SOLVER.get().terminateEarly();
                    yield SimpleScore.ZERO;
                }
                default -> throw new AssertionError("Expected Construction Heuristic to terminate early");
            };
        }
    }

    @NullMarked
    public static final class TestdataConstraintProviderNonzeroValues implements ConstraintProvider {
        @Override
        public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
            return new Constraint[] {
                    constraintFactory.forEach(TestdataEntity.class)
                            .filter(entity -> entity.getValue().getCode().contains("0"))
                            .penalize(SimpleScore.ONE)
                            .asConstraint("Value contains zero")
            };
        }
    }

}
