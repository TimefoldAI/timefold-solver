package ai.timefold.solver.core.impl.score.stream.common.inliner;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.score.TestdataHardSoftBigDecimalScoreSolution;

import org.junit.jupiter.api.Test;

class HardSoftBigDecimalScoreInlinerTest
        extends AbstractScoreInlinerTest<TestdataHardSoftBigDecimalScoreSolution, HardSoftBigDecimalScore> {

    @Test
    void defaultScore() {
        var scoreInliner = buildScoreInliner(Collections.emptyMap(), constraintMatchEnabled);
        assertThat(scoreInliner.extractScore(0)).isEqualTo(HardSoftBigDecimalScore.ZERO);
    }

    @Test
    void impactHard() {
        var constraintWeight = HardSoftBigDecimalScore.ofHard(BigDecimal.valueOf(90));
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardSoftBigDecimalScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(BigDecimal.ONE, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.valueOf(90), BigDecimal.ZERO));

        var undo2 = impacter.impactScore(BigDecimal.valueOf(2), ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.valueOf(270), BigDecimal.ZERO));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.valueOf(90), BigDecimal.ZERO));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.valueOf(0), BigDecimal.ZERO));
    }

    @Test
    void impactSoft() {
        var constraintWeight = HardSoftBigDecimalScore.ofSoft(BigDecimal.valueOf(90));
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardSoftBigDecimalScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(BigDecimal.ONE, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.valueOf(90)));

        var undo2 = impacter.impactScore(BigDecimal.valueOf(2), ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.valueOf(270)));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.valueOf(90)));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void impactAll() {
        var constraintWeight = HardSoftBigDecimalScore.of(BigDecimal.valueOf(10), BigDecimal.valueOf(100));
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardSoftBigDecimalScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(BigDecimal.TEN, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.valueOf(100), BigDecimal.valueOf(1000)));

        var undo2 = impacter.impactScore(BigDecimal.valueOf(20), ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.valueOf(300), BigDecimal.valueOf(3000)));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.valueOf(100), BigDecimal.valueOf(1000)));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Override
    protected SolutionDescriptor<TestdataHardSoftBigDecimalScoreSolution> buildSolutionDescriptor() {
        return TestdataHardSoftBigDecimalScoreSolution.buildSolutionDescriptor();
    }

    @Override
    protected AbstractScoreInliner<HardSoftBigDecimalScore>
            buildScoreInliner(Map<Constraint, HardSoftBigDecimalScore> constraintWeightMap, boolean constraintMatchEnabled) {
        return new HardSoftBigDecimalScoreInliner(constraintWeightMap, constraintMatchEnabled);
    }
}
