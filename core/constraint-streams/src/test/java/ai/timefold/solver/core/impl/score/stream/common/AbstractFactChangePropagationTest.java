package ai.timefold.solver.core.impl.score.stream.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.testdata.domain.chained.shadow.TestdataShadowingChainedAnchor;
import ai.timefold.solver.core.impl.testdata.domain.chained.shadow.TestdataShadowingChainedEntity;
import ai.timefold.solver.core.impl.testdata.domain.chained.shadow.TestdataShadowingChainedSolution;

import org.junit.jupiter.api.Test;

public abstract class AbstractFactChangePropagationTest {

    private static final String VALUE_CODE = "v1";

    private final Solver<TestdataShadowingChainedSolution> solver;

    protected AbstractFactChangePropagationTest(ConstraintStreamImplType constraintStreamImplType) {
        this.solver = buildSolver(constraintStreamImplType);
    }

    /**
     * Tests if Drools try to modify a {@link PlanningEntity} before its shadow variables are updated.
     */
    @Test
    void delayedFactChangePropagation() {
        TestdataShadowingChainedEntity entity = new TestdataShadowingChainedEntity("e1");
        TestdataShadowingChainedAnchor value = new TestdataShadowingChainedAnchor(VALUE_CODE);
        TestdataShadowingChainedSolution inputProblem = new TestdataShadowingChainedSolution();
        inputProblem.setChainedAnchorList(Arrays.asList(value));
        inputProblem.setChainedEntityList(Arrays.asList(entity));

        TestdataShadowingChainedSolution solution = solver.solve(inputProblem);
        TestdataShadowingChainedEntity solvedEntity = solution.getChainedEntityList().get(0);
        assertThat(solvedEntity.getChainedObject()).isNotNull();
        assertThat(solvedEntity.getAnchor().getCode()).isEqualTo(VALUE_CODE);
        assertThat(solution.getScore().isFeasible()).isTrue();
    }

    private Solver<TestdataShadowingChainedSolution> buildSolver(ConstraintStreamImplType constraintStreamImplType) {
        SolverConfig solverConfig = new SolverConfig()
                .withEntityClasses(TestdataShadowingChainedEntity.class)
                .withSolutionClass(TestdataShadowingChainedSolution.class)
                .withConstraintProviderClass(ChainedEntityConstraintProvider.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());
        solverConfig.getScoreDirectorFactoryConfig().setConstraintStreamImplType(constraintStreamImplType);

        SolverFactory<TestdataShadowingChainedSolution> solverFactory = SolverFactory.create(solverConfig);
        return solverFactory.buildSolver();
    }

    public static class ChainedEntityConstraintProvider implements ConstraintProvider {

        @Override
        public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
            return new Constraint[] {
                    anchorCannotBeNull(constraintFactory)
            };
        }

        private Constraint anchorCannotBeNull(ConstraintFactory constraintFactory) {
            return constraintFactory.forEach(TestdataShadowingChainedEntity.class)
                    /*
                     * The getCode() is here just to trigger NPE if the filter's predicate has been called before
                     * the AnchorVariableListener has updated the anchor.
                     */
                    .filter(testdataShadowingChainedEntity -> "v1".equals(testdataShadowingChainedEntity.getAnchor().getCode()))
                    .penalize(SimpleScore.ONE)
                    .asConstraint("anchorCannotBeNull");
        }
    }
}
