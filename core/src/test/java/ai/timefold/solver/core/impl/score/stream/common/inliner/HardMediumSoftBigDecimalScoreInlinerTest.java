package ai.timefold.solver.core.impl.score.stream.common.inliner;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.score.TestdataHardMediumSoftBigDecimalScoreSolution;

import org.junit.jupiter.api.Test;

class HardMediumSoftBigDecimalScoreInlinerTest
        extends AbstractScoreInlinerTest<TestdataHardMediumSoftBigDecimalScoreSolution, HardMediumSoftBigDecimalScore> {

    @Test
    void defaultScore() {
        var scoreInliner = buildScoreInliner(Collections.emptyMap(), constraintMatchEnabled);
        assertThat(scoreInliner.extractScore(0)).isEqualTo(HardMediumSoftBigDecimalScore.ZERO);
    }

    @Test
    void impactHard() {
        var constraintWeight = HardMediumSoftBigDecimalScore.ofHard(BigDecimal.valueOf(90));
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardMediumSoftBigDecimalScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(BigDecimal.ONE, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(BigDecimal.valueOf(90), BigDecimal.ZERO, BigDecimal.ZERO));

        var undo2 = impacter.impactScore(BigDecimal.valueOf(2), ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(BigDecimal.valueOf(270), BigDecimal.ZERO, BigDecimal.ZERO));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(BigDecimal.valueOf(90), BigDecimal.ZERO, BigDecimal.ZERO));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(BigDecimal.valueOf(0), BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void impactMedium() {
        var constraintWeight = HardMediumSoftBigDecimalScore.ofMedium(BigDecimal.valueOf(90));
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardMediumSoftBigDecimalScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(BigDecimal.ONE, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.valueOf(90), BigDecimal.ZERO));

        var undo2 = impacter.impactScore(BigDecimal.valueOf(2), ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.valueOf(270), BigDecimal.ZERO));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.valueOf(90), BigDecimal.ZERO));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void impactSoft() {
        var constraintWeight = HardMediumSoftBigDecimalScore.ofSoft(BigDecimal.valueOf(90));
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardMediumSoftBigDecimalScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(BigDecimal.ONE, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(90)));

        var undo2 = impacter.impactScore(BigDecimal.valueOf(2), ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(270)));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(90)));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void impactAll() {
        var constraintWeight =
                HardMediumSoftBigDecimalScore.of(BigDecimal.valueOf(10), BigDecimal.valueOf(100), BigDecimal.valueOf(1_000));
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardMediumSoftBigDecimalScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(BigDecimal.TEN, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(BigDecimal.valueOf(100), BigDecimal.valueOf(1_000),
                        BigDecimal.valueOf(10_000)));

        var undo2 = impacter.impactScore(BigDecimal.valueOf(20), ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(BigDecimal.valueOf(300), BigDecimal.valueOf(3_000),
                        BigDecimal.valueOf(30_000)));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(BigDecimal.valueOf(100), BigDecimal.valueOf(1_000),
                        BigDecimal.valueOf(10_000)));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Override
    protected SolutionDescriptor<TestdataHardMediumSoftBigDecimalScoreSolution> buildSolutionDescriptor() {
        return TestdataHardMediumSoftBigDecimalScoreSolution.buildSolutionDescriptor();
    }

    @Override
    protected AbstractScoreInliner<HardMediumSoftBigDecimalScore> buildScoreInliner(
            Map<Constraint, HardMediumSoftBigDecimalScore> constraintWeightMap, boolean constraintMatchEnabled) {
        return new HardMediumSoftBigDecimalScoreInliner(constraintWeightMap, constraintMatchEnabled);
    }
}
