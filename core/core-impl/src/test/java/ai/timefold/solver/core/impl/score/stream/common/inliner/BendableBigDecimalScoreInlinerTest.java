package ai.timefold.solver.core.impl.score.stream.common.inliner;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.score.TestdataBendableBigDecimalScoreSolution;

import org.junit.jupiter.api.Test;

class BendableBigDecimalScoreInlinerTest
        extends AbstractScoreInlinerTest<TestdataBendableBigDecimalScoreSolution, BendableBigDecimalScore> {

    @Test
    void defaultScore() {
        var scoreInliner = buildScoreInliner(Collections.emptyMap(), constraintMatchEnabled);
        assertThat(scoreInliner.extractScore(0)).isEqualTo(buildScore(0, 0, 0));
    }

    @Test
    void impactHard() {
        var impacter = buildScoreImpacter(buildScore(90, 0, 0));
        var scoreInliner = (AbstractScoreInliner<BendableBigDecimalScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(BigDecimal.ONE, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(buildScore(90, 0, 0));

        var undo2 = impacter.impactScore(BigDecimal.valueOf(2), ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(buildScore(270, 0, 0));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(buildScore(90, 0, 0));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(buildScore(0, 0, 0));
    }

    @Test
    void impactSoft1() {
        var impacter = buildScoreImpacter(buildScore(0, 90, 0));
        var scoreInliner = (AbstractScoreInliner<BendableBigDecimalScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(BigDecimal.ONE, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(buildScore(0, 90, 0));

        var undo2 = impacter.impactScore(BigDecimal.valueOf(2), ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(buildScore(0, 270, 0));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(buildScore(0, 90, 0));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(buildScore(0, 0, 0));
    }

    @Test
    void impactSoft2() {
        var impacter = buildScoreImpacter(buildScore(0, 0, 90));
        var scoreInliner = (AbstractScoreInliner<BendableBigDecimalScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(BigDecimal.ONE, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(buildScore(0, 0, 90));

        var undo2 = impacter.impactScore(BigDecimal.valueOf(2), ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(buildScore(0, 0, 270));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(buildScore(0, 0, 90));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(buildScore(0, 0, 0));
    }

    @Test
    void impactAll() {
        var impacter = buildScoreImpacter(buildScore(10, 100, 1_000));
        var scoreInliner = (AbstractScoreInliner<BendableBigDecimalScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(BigDecimal.TEN, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(buildScore(100, 1_000, 10_000));

        var undo2 = impacter.impactScore(BigDecimal.valueOf(20), ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(buildScore(300, 3_000, 30_000));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(buildScore(100, 1_000, 10_000));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(buildScore(0, 0, 0));
    }

    @Override
    protected SolutionDescriptor<TestdataBendableBigDecimalScoreSolution> buildSolutionDescriptor() {
        return TestdataBendableBigDecimalScoreSolution.buildSolutionDescriptor();
    }

    @Override
    protected AbstractScoreInliner<BendableBigDecimalScore>
            buildScoreInliner(Map<Constraint, BendableBigDecimalScore> constraintWeightMap, boolean constraintMatchEnabled) {
        return new BendableBigDecimalScoreInliner(constraintWeightMap, constraintMatchEnabled, 1, 2);
    }

    private BendableBigDecimalScore buildScore(long hard, long soft1, long soft2) {
        return BendableBigDecimalScore.of(
                new BigDecimal[] { BigDecimal.valueOf(hard) },
                new BigDecimal[] { BigDecimal.valueOf(soft1), BigDecimal.valueOf(soft2) });
    }

}
