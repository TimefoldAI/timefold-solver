package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListRuinMoveSelectorConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class ListRuinMoveSelectorTest {
    public static class TestdataListConstraintProvider implements ConstraintProvider {

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
    public void testRuining() {
        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withSolutionClass(TestdataListSolution.class)
                .withEntityClasses(TestdataListEntity.class, TestdataListValue.class)
                .withConstraintProviderClass(TestdataListConstraintProvider.class)
                .withPhaseList(List.of(
                        new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig()
                                .withMoveSelectorConfig(new ListRuinMoveSelectorConfig())
                                .withTerminationConfig(new TerminationConfig()
                                        .withStepCountLimit(100))));
        var problem = TestdataListSolution.generateUninitializedSolution(30, 30);
        var solver = SolverFactory.create(solverConfig).buildSolver();
        solver.solve(problem);
    }

    @Test
    public void testRuiningAllowsUnassignedValues() {
        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withSolutionClass(TestdataAllowsUnassignedValuesListSolution.class)
                .withEntityClasses(TestdataAllowsUnassignedValuesListEntity.class,
                        TestdataAllowsUnassignedValuesListValue.class)
                .withConstraintProviderClass(TestdataAllowsUnassignedValuesListConstraintProvider.class)
                .withPhaseList(List.of(
                        new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig()
                                .withMoveSelectorConfig(new ListRuinMoveSelectorConfig())
                                .withTerminationConfig(new TerminationConfig()
                                        .withStepCountLimit(100))));
        var problem = new TestdataAllowsUnassignedValuesListSolution();
        problem.setEntityList(List.of(new TestdataAllowsUnassignedValuesListEntity("A"),
                new TestdataAllowsUnassignedValuesListEntity("B"),
                new TestdataAllowsUnassignedValuesListEntity("C")));
        problem.setValueList(List.of(
                new TestdataAllowsUnassignedValuesListValue("v1"),
                new TestdataAllowsUnassignedValuesListValue("v2"),
                new TestdataAllowsUnassignedValuesListValue("v3"),
                new TestdataAllowsUnassignedValuesListValue("v4"),
                new TestdataAllowsUnassignedValuesListValue("v5")));
        var solver = SolverFactory.create(solverConfig).buildSolver();
        solver.solve(problem);
    }

}