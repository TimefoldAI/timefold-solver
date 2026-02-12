package ai.timefold.solver.core.impl.score.stream.common.inliner;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.testdomain.score.TestdataHardSoftBigDecimalScoreSolution;

import org.junit.jupiter.api.Test;

class HardSoftBigDecimalScoreInlinerTest
        extends AbstractScoreInlinerTest<TestdataHardSoftBigDecimalScoreSolution, HardSoftBigDecimalScore> {

    @Test
    void defaultScore() {
        var scoreInliner = buildScoreInliner(Collections.emptyMap(), constraintMatchPolicy);
        assertThat(scoreInliner.extractScore()).isEqualTo(HardSoftBigDecimalScore.ZERO);
    }

    @Test
    void impactHard() {
        var constraintWeight = HardSoftBigDecimalScore.ofHard(BigDecimal.valueOf(90));
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardSoftBigDecimalScore>) impacter.getContext().inliner;

        var impact1 = impacter.impactScore(BigDecimal.ONE, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.valueOf(90), BigDecimal.ZERO));

        var impact2 = impacter.impactScore(BigDecimal.valueOf(2), ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.valueOf(270), BigDecimal.ZERO));

        impact2.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.valueOf(90), BigDecimal.ZERO));

        impact1.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.valueOf(0), BigDecimal.ZERO));
    }

    @Test
    void impactSoft() {
        var constraintWeight = HardSoftBigDecimalScore.ofSoft(BigDecimal.valueOf(90));
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardSoftBigDecimalScore>) impacter.getContext().inliner;

        var impact1 = impacter.impactScore(BigDecimal.ONE, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.valueOf(90)));

        var impact2 = impacter.impactScore(BigDecimal.valueOf(2), ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.valueOf(270)));

        impact2.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.valueOf(90)));

        impact1.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void impactAll() {
        var constraintWeight = HardSoftBigDecimalScore.of(BigDecimal.valueOf(10), BigDecimal.valueOf(100));
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardSoftBigDecimalScore>) impacter.getContext().inliner;

        var impact1 = impacter.impactScore(BigDecimal.TEN, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.valueOf(100), BigDecimal.valueOf(1000)));

        var impact2 = impacter.impactScore(BigDecimal.valueOf(20), ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.valueOf(300), BigDecimal.valueOf(3000)));

        impact2.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.valueOf(100), BigDecimal.valueOf(1000)));

        impact1.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Override
    protected SolutionDescriptor<TestdataHardSoftBigDecimalScoreSolution> buildSolutionDescriptor() {
        return TestdataHardSoftBigDecimalScoreSolution.buildSolutionDescriptor();
    }

    @Override
    protected AbstractScoreInliner<HardSoftBigDecimalScore>
            buildScoreInliner(Map<Constraint, HardSoftBigDecimalScore> constraintWeightMap,
                    ConstraintMatchPolicy constraintMatchPolicy) {
        return new HardSoftBigDecimalScoreInliner(constraintWeightMap, constraintMatchPolicy);
    }
}
