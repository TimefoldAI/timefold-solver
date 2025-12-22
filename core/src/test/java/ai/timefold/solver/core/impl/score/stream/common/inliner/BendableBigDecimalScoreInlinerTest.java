package ai.timefold.solver.core.impl.score.stream.common.inliner;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.testdomain.score.TestdataBendableBigDecimalScoreSolution;

import org.junit.jupiter.api.Test;

class BendableBigDecimalScoreInlinerTest
        extends AbstractScoreInlinerTest<TestdataBendableBigDecimalScoreSolution, BendableBigDecimalScore> {

    @Test
    void defaultScore() {
        var scoreInliner = buildScoreInliner(Collections.emptyMap(), constraintMatchPolicy);
        assertThat(scoreInliner.extractScore()).isEqualTo(buildScore(0, 0, 0));
    }

    @Test
    void impactHard() {
        var impacter = buildScoreImpacter(buildScore(90, 0, 0));
        var scoreInliner = (AbstractScoreInliner<BendableBigDecimalScore>) impacter.getContext().parent;

        var impact1 = impacter.impactScore(BigDecimal.ONE, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(90, 0, 0));

        var impact2 = impacter.impactScore(BigDecimal.valueOf(2), ConstraintMatchSupplier.empty());
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
        var impacter = buildScoreImpacter(buildScore(0, 90, 0));
        var scoreInliner = (AbstractScoreInliner<BendableBigDecimalScore>) impacter.getContext().parent;

        var impact1 = impacter.impactScore(BigDecimal.ONE, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 90, 0));

        var impact2 = impacter.impactScore(BigDecimal.valueOf(2), ConstraintMatchSupplier.empty());
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
        var impacter = buildScoreImpacter(buildScore(0, 0, 90));
        var scoreInliner = (AbstractScoreInliner<BendableBigDecimalScore>) impacter.getContext().parent;

        var impact1 = impacter.impactScore(BigDecimal.ONE, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 90));

        var impact2 = impacter.impactScore(BigDecimal.valueOf(2), ConstraintMatchSupplier.empty());
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
        var impacter = buildScoreImpacter(buildScore(10, 100, 1_000));
        var scoreInliner = (AbstractScoreInliner<BendableBigDecimalScore>) impacter.getContext().parent;

        var impact1 = impacter.impactScore(BigDecimal.TEN, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(100, 1_000, 10_000));

        var impact2 = impacter.impactScore(BigDecimal.valueOf(20), ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(300, 3_000, 30_000));

        impact2.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(100, 1_000, 10_000));

        impact1.undo();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 0));
    }

    @Override
    protected SolutionDescriptor<TestdataBendableBigDecimalScoreSolution> buildSolutionDescriptor() {
        return TestdataBendableBigDecimalScoreSolution.buildSolutionDescriptor();
    }

    @Override
    protected AbstractScoreInliner<BendableBigDecimalScore>
            buildScoreInliner(Map<Constraint, BendableBigDecimalScore> constraintWeightMap,
                    ConstraintMatchPolicy constraintMatchPolicy) {
        return new BendableBigDecimalScoreInliner(constraintWeightMap, constraintMatchPolicy, 1, 2);
    }

    private BendableBigDecimalScore buildScore(long hard, long soft1, long soft2) {
        return BendableBigDecimalScore.of(
                new BigDecimal[] { BigDecimal.valueOf(hard) },
                new BigDecimal[] { BigDecimal.valueOf(soft1), BigDecimal.valueOf(soft2) });
    }

}
