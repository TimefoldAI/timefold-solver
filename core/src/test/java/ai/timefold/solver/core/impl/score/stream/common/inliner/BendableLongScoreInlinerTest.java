package ai.timefold.solver.core.impl.score.stream.common.inliner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.testdomain.score.TestdataBendableLongScoreSolution;

import org.junit.jupiter.api.Test;

class BendableLongScoreInlinerTest extends AbstractScoreInlinerTest<TestdataBendableLongScoreSolution, BendableLongScore> {

    @Test
    void defaultScore() {
        var scoreInliner = buildScoreInliner(Collections.emptyMap(), constraintMatchPolicy);
        assertThat(scoreInliner.extractScore()).isEqualTo(buildScore(0, 0, 0));
    }

    @Test
    void impactHard() {
        var constraintWeight = buildScore(90, 0, 0);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<BendableLongScore>) impacter.getContext().inliner;

        var impact1 = impacter.impactScore(1, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(90, 0, 0));

        var impact2 = impacter.impactScore(2, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(270, 0, 0));

        impact2.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(90, 0, 0));

        impact1.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 0));
    }

    @Test
    void impactSoft1() {
        var constraintWeight = buildScore(0, 90, 0);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<BendableLongScore>) impacter.getContext().inliner;

        var impact1 = impacter.impactScore(1, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 90, 0));

        var impact2 = impacter.impactScore(2, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 270, 0));

        impact2.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 90, 0));

        impact1.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 0));
    }

    @Test
    void impactSoft2() {
        var constraintWeight = buildScore(0, 0, 90);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<BendableLongScore>) impacter.getContext().inliner;

        var impact1 = impacter.impactScore(1, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 90));

        var impact2 = impacter.impactScore(2, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 270));

        impact2.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 90));

        impact1.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 0));
    }

    @Test
    void impactAll() {
        var constraintWeight = buildScore(10, 100, 1_000);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<BendableLongScore>) impacter.getContext().inliner;

        var impact1 = impacter.impactScore(10, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(100, 1_000, 10_000));

        var impact2 = impacter.impactScore(20, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(300, 3_000, 30_000));

        impact2.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(100, 1_000, 10_000));

        impact1.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 0));
    }

    @Test
    void impactAllMatchWeightOverflow() {
        var constraintWeight = buildScore(100, 1_000, 10_000);
        var impacter = buildScoreImpacter(constraintWeight);
        assertThatThrownBy(() -> impacter.impactScore(Long.MAX_VALUE, ConstraintMatchSupplier.empty()))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void impactAllTotalOverflow() {
        var constraintWeight = buildScore(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
        var impacter = buildScoreImpacter(constraintWeight);
        impacter.impactScore(1, ConstraintMatchSupplier.empty()); // This will send the total right to the limit.
        assertThatThrownBy(() -> impacter.impactScore(1, ConstraintMatchSupplier.empty()))
                .isInstanceOf(ArithmeticException.class);
    }

    @Override
    protected SolutionDescriptor<TestdataBendableLongScoreSolution> buildSolutionDescriptor() {
        return TestdataBendableLongScoreSolution.buildSolutionDescriptor();
    }

    @Override
    protected AbstractScoreInliner<BendableLongScore> buildScoreInliner(Map<Constraint, BendableLongScore> constraintWeightMap,
            ConstraintMatchPolicy constraintMatchPolicy) {
        return new BendableLongScoreInliner(constraintWeightMap, constraintMatchPolicy, 1, 2);
    }

    private BendableLongScore buildScore(long hard, long soft1, long soft2) {
        return BendableLongScore.of(
                new long[] { hard },
                new long[] { soft1, soft2 });
    }

}
