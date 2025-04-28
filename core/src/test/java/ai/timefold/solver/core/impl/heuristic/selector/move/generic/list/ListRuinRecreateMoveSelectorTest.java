package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListRuinRecreateMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.testutil.AbstractMeterTest;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Metrics;

class ListRuinRecreateMoveSelectorTest extends AbstractMeterTest {

    public static final class TestdataListConstraintProvider implements ConstraintProvider {

        @Override
        public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
            return new Constraint[] {
                    constraintFactory.forEach(TestdataListValue.class)
                            .penalize(SimpleScore.ONE, value -> Math.abs(
                                    Objects.hash(value.getEntity().getCode().hashCode(),
                                            value.getIndex(),
                                            value.getCode())) >> 8)
                            .asConstraint("Hash constraint")
            };
        }
    }

    @Test
    void testRuining() {
        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withSolutionClass(TestdataListSolution.class)
                .withEntityClasses(TestdataListEntity.class, TestdataListValue.class)
                .withConstraintProviderClass(TestdataListConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig()
                        .withUnimprovedMillisecondsSpentLimit(1000L))
                .withPhaseList(List.of(
                        new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig()
                                .withMoveSelectorConfig(new ListRuinRecreateMoveSelectorConfig())));
        var problem = TestdataListSolution.generateUninitializedSolution(10, 3);
        var solver = SolverFactory.create(solverConfig).buildSolver();
        assertDoesNotThrow(() -> solver.solve(problem));
    }

    @Test
    void testRuiningWithMetric() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withSolutionClass(TestdataListSolution.class)
                .withEntityClasses(TestdataListEntity.class, TestdataListValue.class)
                .withConstraintProviderClass(TestdataListConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig()
                        .withUnimprovedMillisecondsSpentLimit(1000L))
                .withPhaseList(List.of(
                        new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig()
                                .withMoveSelectorConfig(new ListRuinRecreateMoveSelectorConfig())));
        var problem = TestdataListSolution.generateUninitializedSolution(10, 3);
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

    public static final class TestdataAllowsUnassignedValuesListMixedConstraintProvider
            implements ConstraintProvider {

        @Override
        public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
            return new Constraint[] {
                    constraintFactory.forEach(TestdataAllowsUnassignedValuesListEntity.class)
                            .penalize(SimpleScore.ONE, entity -> entity.getValueList().size() * entity.getValueList().size())
                            .asConstraint("Minimize entity list size"),
                    constraintFactory.forEachIncludingUnassigned(TestdataAllowsUnassignedValuesListValue.class)
                            .filter(value -> value.getEntity() == null)
                            .penalize(SimpleScore.of(5))
                            .asConstraint("Minimize unassigned values"),
                    constraintFactory.forEachUniquePair(TestdataAllowsUnassignedValuesListValue.class,
                            Joiners.equal(TestdataAllowsUnassignedValuesListValue::getIndex))
                            .penalize(SimpleScore.ONE)
                            .asConstraint("Maximize different indices")
            };
        }
    }

    @Test
    void testRuiningAllowsUnassignedValues() {
        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withSolutionClass(TestdataAllowsUnassignedValuesListSolution.class)
                .withEntityClasses(TestdataAllowsUnassignedValuesListEntity.class,
                        TestdataAllowsUnassignedValuesListValue.class)
                .withConstraintProviderClass(TestdataAllowsUnassignedValuesListMixedConstraintProvider.class)
                .withPhaseList(List.of(
                        new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig()
                                .withMoveSelectorConfig(new UnionMoveSelectorConfig(List.of(
                                        new ListChangeMoveSelectorConfig()
                                                .withFixedProbabilityWeight(0.4),
                                        new ListSwapMoveSelectorConfig()
                                                .withFixedProbabilityWeight(0.4),
                                        new ListRuinRecreateMoveSelectorConfig()
                                                .withFixedProbabilityWeight(0.2))))
                                .withTerminationConfig(new TerminationConfig()
                                        .withStepCountLimit(100))));
        var problem = new TestdataAllowsUnassignedValuesListSolution();
        problem.setEntityList(IntStream.range(0, 3)
                .mapToObj(id -> new TestdataAllowsUnassignedValuesListEntity("e" + id))
                .toList());
        problem.setValueList(IntStream.range(0, 10)
                .mapToObj(id -> new TestdataAllowsUnassignedValuesListValue("v" + id))
                .toList());
        var solver = SolverFactory.create(solverConfig).buildSolver();
        assertDoesNotThrow(() -> solver.solve(problem));
    }

}
