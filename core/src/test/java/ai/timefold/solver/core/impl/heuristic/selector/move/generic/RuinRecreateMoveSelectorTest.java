package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.RuinRecreateMoveSelectorConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.testdata.domain.TestdataConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.allows_unassigned.TestdataAllowsUnassignedEasyScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.allows_unassigned.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.impl.testdata.domain.allows_unassigned.TestdataAllowsUnassignedSolution;
import ai.timefold.solver.core.impl.testutil.TestMeterRegistry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import io.micrometer.core.instrument.Metrics;

@Execution(ExecutionMode.CONCURRENT)
class RuinRecreateMoveSelectorTest {

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
        solver.addEventListener(event -> meterRegistry.publish(solver));
        solver.solve(problem);

        SolverMetric.MOVE_EVALUATION_COUNT.register(solver);
        SolverMetric.SCORE_CALCULATION_COUNT.register(solver);
        meterRegistry.publish(solver);
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

}
