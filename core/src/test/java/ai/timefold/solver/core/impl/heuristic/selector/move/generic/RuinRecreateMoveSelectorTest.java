package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.RuinRecreateMoveSelectorConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.testdomain.TestdataConstraintProvider;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;
import ai.timefold.solver.core.testutil.AbstractMeterTest;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Metrics;

class RuinRecreateMoveSelectorTest extends AbstractMeterTest {

    @Test
    void testRuining() {
        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withConstraintProviderClass(TestdataConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig()
                        .withUnimprovedMillisecondsSpentLimit(1000L))
                .withPhaseList(List.of(
                        new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig()
                                .withMoveSelectorConfig(new RuinRecreateMoveSelectorConfig())));
        var problem = TestdataSolution.generateSolution(5, 30);
        var solver = SolverFactory.create(solverConfig).buildSolver();
        assertDoesNotThrow(() -> solver.solve(problem));
    }

    @Test
    void testRuiningWithMetric() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withConstraintProviderClass(TestdataConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig()
                        .withUnimprovedMillisecondsSpentLimit(1000L))
                .withPhaseList(List.of(
                        new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig()
                                .withMoveSelectorConfig(new RuinRecreateMoveSelectorConfig())));
        var problem = TestdataSolution.generateSolution(5, 30);
        var solver = SolverFactory.create(solverConfig).buildSolver();
        solver.addEventListener(event -> meterRegistry.publish());
        solver.solve(problem);

        SolverMetric.MOVE_EVALUATION_COUNT.register(solver);
        SolverMetric.SCORE_CALCULATION_COUNT.register(solver);
        meterRegistry.publish();
        var scoreCount = meterRegistry.getMeasurement(SolverMetric.SCORE_CALCULATION_COUNT.getMeterId(), "VALUE");
        var moveCount = meterRegistry.getMeasurement(SolverMetric.MOVE_EVALUATION_COUNT.getMeterId(), "VALUE");
        assertThat(scoreCount).isPositive();
        assertThat(moveCount).isPositive();
        assertThat(scoreCount).isGreaterThan(moveCount);
    }

    @Test
    void testRuiningAllowsUnassigned() {
        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withSolutionClass(TestdataAllowsUnassignedSolution.class)
                .withEntityClasses(TestdataAllowsUnassignedEntity.class)
                .withEasyScoreCalculatorClass(TestdataAllowsUnassignedEasyScoreCalculator.class)
                .withPhaseList(List.of(
                        new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig()
                                .withMoveSelectorConfig(new RuinRecreateMoveSelectorConfig())
                                .withTerminationConfig(new TerminationConfig()
                                        .withStepCountLimit(100))));
        var problem = TestdataAllowsUnassignedSolution.generateSolution(5, 30);
        var solver = SolverFactory.create(solverConfig).buildSolver();
        assertDoesNotThrow(() -> solver.solve(problem));
    }

    @Test
    void testRuiningNoAssignedValues() {
        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withSolutionClass(TestdataAllowsUnassignedSolution.class)
                .withEntityClasses(TestdataAllowsUnassignedEntity.class)
                .withConstraintProviderClass(TestdataNoAssignedValuesListMixedConstraintProvider.class)
                .withPhaseList(List.of(
                        new LocalSearchPhaseConfig()
                                // Since the problem has 3 planning entities, the R&R will select all of them
                                .withMoveSelectorConfig(new RuinRecreateMoveSelectorConfig().withMinimumRuinedCount(3))
                                .withTerminationConfig(new TerminationConfig()
                                        .withStepCountLimit(1))));
        // The problem involves three entities and three planning values,
        // with the initial solution assigning a value to each entity
        var problem = TestdataAllowsUnassignedSolution.generateSolution(3, 3);
        for (var i = 0; i < 3; i++) {
            problem.getEntityList().get(i).setValue(problem.getValueList().get(i));
        }
        var solver = SolverFactory.create(solverConfig).buildSolver();
        // All values must remain unassigned
        var solution = (TestdataAllowsUnassignedSolution) assertDoesNotThrow(() -> solver.solve(problem));
        assertThat(solution.getEntityList().get(0).getValue()).isNull();
        assertThat(solution.getEntityList().get(1).getValue()).isNull();
        assertThat(solution.getEntityList().get(2).getValue()).isNull();
    }

    public static final class TestdataNoAssignedValuesListMixedConstraintProvider
            implements ConstraintProvider {

        @Override
        public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
            return new Constraint[] {
                    constraintFactory.forEach(TestdataAllowsUnassignedEntity.class)
                            .penalize(SimpleScore.ONE)
                            .asConstraint("No Assigned")
            };
        }
    }

}
