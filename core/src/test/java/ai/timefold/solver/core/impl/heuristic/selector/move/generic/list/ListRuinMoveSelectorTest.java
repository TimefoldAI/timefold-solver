package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.List;

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

import org.junit.jupiter.api.Test;

class ListRuinMoveSelectorTest {
    public static class TestdataListConstraintProvider implements ConstraintProvider {

        @Override
        public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
            return new Constraint[] {
            };
        }
    }

    @Test
    public void testRuining() {
        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withSolutionClass(TestdataListSolution.class)
                .withEntityClasses(TestdataListEntity.class, TestdataListValue.class)
                .withConstraintProviderClass(TestdataListConstraintProvider.class)
                .withPhaseList(List.of(
                        new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig()
                                .withMoveSelectorConfig(
                                        new ListRuinMoveSelectorConfig())
                                .withTerminationConfig(new TerminationConfig()
                                        .withStepCountLimit(20))));
        var problem = TestdataListSolution.generateUninitializedSolution(30, 30);
        var solver = SolverFactory.create(solverConfig).buildSolver();
        solver.solve(problem);
    }
}