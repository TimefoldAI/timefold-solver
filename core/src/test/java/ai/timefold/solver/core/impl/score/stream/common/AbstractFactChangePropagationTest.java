package ai.timefold.solver.core.impl.score.stream.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.testdomain.chained.shadow.TestdataShadowingChainedAnchor;
import ai.timefold.solver.core.testdomain.chained.shadow.TestdataShadowingChainedEntity;
import ai.timefold.solver.core.testdomain.chained.shadow.TestdataShadowingChainedSolution;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

public abstract class AbstractFactChangePropagationTest {

    private static final String VALUE_CODE = "v1";

    private final Solver<TestdataShadowingChainedSolution> solver;

    protected AbstractFactChangePropagationTest() {
        this.solver = buildSolver();
    }

    /**
     * Tests if Drools try to modify a {@link PlanningEntity} before its shadow variables are updated.
     */
    @Test
    void delayedFactChangePropagation() {
        var entity = new TestdataShadowingChainedEntity("e1");
        var value = new TestdataShadowingChainedAnchor(VALUE_CODE);
        var inputProblem = new TestdataShadowingChainedSolution();
        inputProblem.setChainedAnchorList(List.of(value));
        inputProblem.setChainedEntityList(List.of(entity));

        var solution = solver.solve(inputProblem);
        var solvedEntity = solution.getChainedEntityList().get(0);
        assertThat(solvedEntity.getChainedObject()).isNotNull();
        assertThat(solvedEntity.getAnchor().getCode()).isEqualTo(VALUE_CODE);
        assertThat(solution.getScore().isFeasible()).isTrue();
    }

    private Solver<TestdataShadowingChainedSolution> buildSolver() {
        var solverConfig = new SolverConfig()
                .withEntityClasses(TestdataShadowingChainedEntity.class)
                .withSolutionClass(TestdataShadowingChainedSolution.class)
                .withConstraintProviderClass(ChainedEntityConstraintProvider.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());
        var solverFactory = SolverFactory.<TestdataShadowingChainedSolution> create(solverConfig);
        return solverFactory.buildSolver();
    }

    public static class ChainedEntityConstraintProvider implements ConstraintProvider {

        @Override
        public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
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
