package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

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
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class ListRuinRecreateMoveSelectorTest {

    public static final class TestdataListConstraintProvider implements ConstraintProvider {

        @Override
        public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
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
                .withPhaseList(List.of(
                        new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig()
                                .withMoveSelectorConfig(new ListRuinRecreateMoveSelectorConfig())
                                .withTerminationConfig(new TerminationConfig()
                                        .withStepCountLimit(100))));
        var problem = TestdataListSolution.generateUninitializedSolution(10, 3);
        var solver = SolverFactory.create(solverConfig).buildSolver();
        solver.solve(problem);
    }

    public static final class TestdataAllowsUnassignedValuesListMixedConstraintProvider
            implements ConstraintProvider {

        @Override
        public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
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
        solver.solve(problem);
    }

}