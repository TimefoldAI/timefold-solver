package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.List;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.RuinMoveSelectorConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.testdata.domain.TestdataConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

public class RuinMoveSelectorTest {
    @Test
    public void testRuining() {
        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withConstraintProviderClass(TestdataConstraintProvider.class)
                .withPhaseList(List.of(
                        new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig()
                                .withMoveSelectorConfig(
                                        new RuinMoveSelectorConfig())
                                .withTerminationConfig(new TerminationConfig()
                                        .withStepCountLimit(20))));
        var problem = TestdataSolution.generateSolution(30, 30);
        var solver = SolverFactory.create(solverConfig).buildSolver();
        solver.solve(problem);
    }
}
