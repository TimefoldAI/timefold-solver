package ai.timefold.solver.core.impl.score.stream.common.inliner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.testdomain.score.TestdataHardMediumSoftLongScoreSolution;

import org.junit.jupiter.api.Test;

class HardMediumSoftLongScoreInlinerTest
        extends AbstractScoreInlinerTest<TestdataHardMediumSoftLongScoreSolution, HardMediumSoftLongScore> {

    @Test
    void defaultScore() {
        var scoreInliner = buildScoreInliner(Collections.emptyMap(), constraintMatchPolicy);
        assertThat(scoreInliner.extractScore()).isEqualTo(HardMediumSoftLongScore.ZERO);
    }

    @Test
    void impactHard() {
        var constraintWeight = HardMediumSoftLongScore.ofHard(90);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardMediumSoftLongScore>) impacter.getContext().inliner;

        var impact1 = impacter.impactScore(1, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardMediumSoftLongScore.of(90, 0, 0));

        var impact2 = impacter.impactScore(2, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardMediumSoftLongScore.of(270, 0, 0));

        impact2.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardMediumSoftLongScore.of(90, 0, 0));

        impact1.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardMediumSoftLongScore.of(0, 0, 0));
    }

    @Test
    void impactMedium() {
        var constraintWeight = HardMediumSoftLongScore.ofMedium(90);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardMediumSoftLongScore>) impacter.getContext().inliner;

        var impact1 = impacter.impactScore(1, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardMediumSoftLongScore.of(0, 90, 0));

        var impact2 = impacter.impactScore(2, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardMediumSoftLongScore.of(0, 270, 0));

        impact2.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardMediumSoftLongScore.of(0, 90, 0));

        impact1.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardMediumSoftLongScore.of(0, 0, 0));
    }

    @Test
    void impactSoft() {
        var constraintWeight = HardMediumSoftLongScore.ofSoft(90);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardMediumSoftLongScore>) impacter.getContext().inliner;

        var impact1 = impacter.impactScore(1, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardMediumSoftLongScore.of(0, 0, 90));

        var impact2 = impacter.impactScore(2, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardMediumSoftLongScore.of(0, 0, 270));

        impact2.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardMediumSoftLongScore.of(0, 0, 90));

        impact1.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardMediumSoftLongScore.of(0, 0, 0));
    }

    @Test
    void impactAll() {
        var constraintWeight = HardMediumSoftLongScore.of(10, 100, 1_000);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardMediumSoftLongScore>) impacter.getContext().inliner;

        var impact1 = impacter.impactScore(10, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardMediumSoftLongScore.of(100, 1_000, 10_000));

        var impact2 = impacter.impactScore(20, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardMediumSoftLongScore.of(300, 3_000, 30_000));

        impact2.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardMediumSoftLongScore.of(100, 1_000, 10_000));

        impact1.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardMediumSoftLongScore.of(0, 0, 0));
    }

    @Test
    void impactAllMatchWeightOverflow() {
        var constraintWeight = HardMediumSoftLongScore.of(10, 100, 1_000);
        var impacter = buildScoreImpacter(constraintWeight);
        assertThatThrownBy(() -> impacter.impactScore(Long.MAX_VALUE, ConstraintMatchSupplier.empty()))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void impactAllTotalOverflow() {
        var constraintWeight = HardMediumSoftLongScore.of(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
        var impacter = buildScoreImpacter(constraintWeight);
        impacter.impactScore(1, ConstraintMatchSupplier.empty()); // This will send the total right to the limit.
        assertThatThrownBy(() -> impacter.impactScore(1, ConstraintMatchSupplier.empty()))
                .isInstanceOf(ArithmeticException.class);
    }

    @Override
    protected SolutionDescriptor<TestdataHardMediumSoftLongScoreSolution> buildSolutionDescriptor() {
        return TestdataHardMediumSoftLongScoreSolution.buildSolutionDescriptor();
    }

    @Override
    protected AbstractScoreInliner<HardMediumSoftLongScore>
            buildScoreInliner(Map<Constraint, HardMediumSoftLongScore> constraintWeightMap,
                    ConstraintMatchPolicy constraintMatchPolicy) {
        return new HardMediumSoftLongScoreInliner(constraintWeightMap, constraintMatchPolicy);
    }
}
