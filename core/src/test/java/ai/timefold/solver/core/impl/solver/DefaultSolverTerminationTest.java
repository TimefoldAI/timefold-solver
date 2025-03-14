package ai.timefold.solver.core.impl.solver;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

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
import ai.timefold.solver.core.config.solver.termination.TerminationCompositionStyle;
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
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
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

    @ParameterizedTest
    @ArgumentsSource(TerminationArgumentSource.class)
    @Timeout(10)
    void terminateEarlyConstructionHeuristic(TerminationConfig terminationConfig) {
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class)
                .withEasyScoreCalculatorClass(DummySimpleScoreThrowingEasyScoreCalculator.class)
                .withTerminationConfig(terminationConfig) // Long enough to never trigger.
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

    @ParameterizedTest
    @ArgumentsSource(TerminationArgumentSource.class)
    @Timeout(10)
    void terminateEarlyConstructionHeuristicInterrupted(TerminationConfig terminationConfig) {
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class)
                .withEasyScoreCalculatorClass(DummySimpleScoreInterruptingEasyScoreCalculator.class)
                .withTerminationConfig(terminationConfig) // Long enough to never trigger.
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

    @ParameterizedTest
    @ArgumentsSource(TerminationArgumentSource.class)
    @Timeout(10)
    void terminateEarlyLocalSearch(TerminationConfig terminationConfig) {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withTerminationConfig(terminationConfig) // Long enough to never trigger.
                .withConstraintProviderClass(TestdataConstraintProviderNonzeroValues.class)
                .withPhases(new LocalSearchPhaseConfig());

        var solution = TestdataSolution.generateSolution(2, 2);
        var solver = SolverFactory.<TestdataSolution> create(solverConfig)
                .buildSolver();
        solver.addEventListener(event -> {
            solver.terminateEarly(); // Once the solver is running, terminate it.
            LOGGER.info("Sent request to terminate early.");
        });

        var resultingSolution = solver.solve(solution);
        Assertions.assertThat(resultingSolution).isNotNull();
    }

    @ParameterizedTest
    @ArgumentsSource(TerminationArgumentSource.class)
    @Timeout(10)
    void terminateEarlyLocalSearchInterrupted(TerminationConfig terminationConfig) {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withTerminationConfig(terminationConfig) // Long enough to never trigger.
                .withConstraintProviderClass(TestdataConstraintProviderNonzeroValues.class)
                .withPhases(new LocalSearchPhaseConfig());

        var solution = TestdataSolution.generateSolution(2, 2);
        var solver = SolverFactory.<TestdataSolution> create(solverConfig)
                .buildSolver();
        solver.addEventListener(event -> {
            Thread.currentThread().interrupt(); // Once the solver is running, terminate it.
            LOGGER.info("Sent request to terminate early.");
        });

        var resultingSolution = solver.solve(solution);
        Assertions.assertThat(resultingSolution).isNotNull();
    }

    static class TerminationArgumentSource implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of(new TerminationConfig().withStepCountLimit(10000)),
                    Arguments.of(new TerminationConfig().withScoreCalculationCountLimit(10000L)),
                    Arguments.of(new TerminationConfig().withBestScoreLimit("1000")),
                    Arguments.of(new TerminationConfig().withDaysSpentLimit(10L)),
                    Arguments.of(new TerminationConfig().withDiminishedReturns()),
                    Arguments.of(new TerminationConfig().withUnimprovedDaysSpentLimit(10L)),
                    Arguments.of(new TerminationConfig().withUnimprovedStepCountLimit(10000)),
                    Arguments.of(new TerminationConfig().withMoveCountLimit(10000L)),
                    Arguments.of(new TerminationConfig()
                            .withStepCountLimit(10000)
                            .withMoveCountLimit(10000L)
                            .withTerminationCompositionStyle(TerminationCompositionStyle.OR)),
                    Arguments.of(new TerminationConfig()
                            .withStepCountLimit(10000)
                            .withMoveCountLimit(10000L)
                            .withTerminationCompositionStyle(TerminationCompositionStyle.AND)));
        }
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
    public static final class DummySimpleScoreInterruptingEasyScoreCalculator
            implements EasyScoreCalculator<TestdataSolution, SimpleScore> {
        @Override
        public SimpleScore calculateScore(TestdataSolution solution) {
            var unassignedCount = (int) solution.getEntityList().stream()
                    .filter(entity -> entity.getValue() == null)
                    .count();
            return switch (unassignedCount) {
                case 2 -> { // CH is starting, ensure we are in the right state.
                    yield SimpleScore.ZERO;
                }
                case 1 -> { // CH did one step, guaranteed running.
                    Thread.currentThread().interrupt();
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
