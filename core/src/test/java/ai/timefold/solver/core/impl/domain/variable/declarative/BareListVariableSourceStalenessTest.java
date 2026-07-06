package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

/**
 * Demonstrates whether a declarative shadow variable with a bare planning list variable
 * source, i.e. {@code @ShadowSources("values")}, is retriggered when the list variable
 * changes during solving.
 * The javadoc of {@code @ShadowVariable} documents a genuine {@code @PlanningListVariable}
 * as a valid source.
 */
class BareListVariableSourceStalenessTest {

    @Test
    void bareListVariableSourceIsRetriggeredDuringSolving() {
        var problem = new Solution();
        problem.owners = List.of(new Owner("o1"), new Owner("o2"));
        problem.vals = List.of(new Val("v1"), new Val("v2"), new Val("v3"));

        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.PHASE_ASSERT)
                .withSolutionClass(Solution.class)
                .withEntityClasses(Owner.class)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withEasyScoreCalculatorClass(AssertingCalculator.class))
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(100L));

        var solution = SolverFactory.<Solution> create(solverConfig).buildSolver().solve(problem);

        assertThat(solution).isNotNull();
        for (var owner : solution.owners) {
            assertThat(owner.valueCount).isEqualTo(owner.values.size());
        }
    }

    public static class AssertingCalculator implements EasyScoreCalculator<Solution, SimpleScore> {
        @Override
        public @NonNull SimpleScore calculateScore(@NonNull Solution solution) {
            var total = 0;
            for (var owner : solution.owners) {
                if (owner.valueCount == null || owner.valueCount != owner.values.size()) {
                    throw new IllegalStateException(
                            "Stale shadow: owner (%s) has valueCount (%s) but its list has size (%d)."
                                    .formatted(owner.id, owner.valueCount, owner.values.size()));
                }
                total += owner.valueCount;
            }
            return SimpleScore.of(total);
        }
    }

    @PlanningSolution
    public static class Solution {
        @PlanningEntityCollectionProperty
        public List<Owner> owners;
        @ProblemFactCollectionProperty
        @ValueRangeProvider
        public List<Val> vals;
        @PlanningScore
        public SimpleScore score;
    }

    @PlanningEntity
    public static class Owner {
        @PlanningId
        public String id;

        @PlanningListVariable
        public List<Val> values = new ArrayList<>();

        @ShadowVariable(supplierName = "countSupplier")
        public Integer valueCount;

        public Owner() {
        }

        public Owner(String id) {
            this.id = id;
        }

        @ShadowSources("values")
        public Integer countSupplier() {
            return values.size();
        }
    }

    public static class Val {
        @PlanningId
        public String id;

        public Val() {
        }

        public Val(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return id;
        }
    }
}
