package ai.timefold.solver.core.impl.score.stream.common.inliner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.testdomain.score.TestdataSimpleLongScoreSolution;

import org.junit.jupiter.api.Test;

class SimpleLongScoreInlinerTest extends AbstractScoreInlinerTest<TestdataSimpleLongScoreSolution, SimpleLongScore> {

    @Test
    void defaultScore() {
        var scoreInliner = buildScoreInliner(Collections.emptyMap(), constraintMatchPolicy);
        assertThat(scoreInliner.extractScore()).isEqualTo(SimpleLongScore.ZERO);
    }

    @Test
    void impact() {
        var constraintWeight = SimpleLongScore.of(10);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<SimpleLongScore>) impacter.getContext().inliner;

        var impact1 = impacter.impactScore(10, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(SimpleLongScore.of(100));

        var impact2 = impacter.impactScore(20, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(SimpleLongScore.of(300));

        impact2.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(SimpleLongScore.of(100));

        impact1.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(SimpleLongScore.of(0));
    }

    @Test
    void impactMatchWeightOverflow() {
        var constraintWeight = SimpleLongScore.of(10);
        var impacter = buildScoreImpacter(constraintWeight);
        assertThatThrownBy(() -> impacter.impactScore(Long.MAX_VALUE, ConstraintMatchSupplier.empty()))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void impactTotalOverflow() {
        var constraintWeight = SimpleLongScore.of(Long.MAX_VALUE);
        var impacter = buildScoreImpacter(constraintWeight);
        impacter.impactScore(1, ConstraintMatchSupplier.empty()); // This will send the total right to the limit.
        assertThatThrownBy(() -> impacter.impactScore(1, ConstraintMatchSupplier.empty()))
                .isInstanceOf(ArithmeticException.class);
    }

    @Override
    protected SolutionDescriptor<TestdataSimpleLongScoreSolution> buildSolutionDescriptor() {
        return TestdataSimpleLongScoreSolution.buildSolutionDescriptor();
    }

    @Override
    protected AbstractScoreInliner<SimpleLongScore> buildScoreInliner(Map<Constraint, SimpleLongScore> constraintWeightMap,
            ConstraintMatchPolicy constraintMatchPolicy) {
        return new SimpleLongScoreInliner(constraintWeightMap, constraintMatchPolicy);
    }
}
