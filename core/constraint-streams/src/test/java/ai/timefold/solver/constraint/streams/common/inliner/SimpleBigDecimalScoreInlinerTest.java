package ai.timefold.solver.constraint.streams.common.inliner;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.score.TestdataSimpleBigDecimalScoreSolution;

import org.junit.jupiter.api.Test;

class SimpleBigDecimalScoreInlinerTest
        extends AbstractScoreInlinerTest<TestdataSimpleBigDecimalScoreSolution, SimpleBigDecimalScore> {

    @Test
    void defaultScore() {
        SimpleBigDecimalScoreInliner scoreInliner =
                new SimpleBigDecimalScoreInliner(constraintMatchEnabled);
        assertThat(scoreInliner.extractScore(0)).isEqualTo(SimpleBigDecimalScore.ZERO);
    }

    @Test
    void impact() {
        SimpleBigDecimalScoreInliner scoreInliner =
                new SimpleBigDecimalScoreInliner(constraintMatchEnabled);

        SimpleBigDecimalScore constraintWeight = SimpleBigDecimalScore.of(BigDecimal.valueOf(10));
        WeightedScoreImpacter<SimpleBigDecimalScoreContext> hardImpacter =
                scoreInliner.buildWeightedScoreImpacter(buildConstraint(constraintWeight), constraintWeight);
        UndoScoreImpacter undo1 = hardImpacter.impactScore(BigDecimal.TEN, JustificationsSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(100)));

        UndoScoreImpacter undo2 = hardImpacter.impactScore(BigDecimal.valueOf(20), JustificationsSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(300)));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(100)));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(SimpleBigDecimalScore.of(BigDecimal.ZERO));
    }

    @Override
    protected SolutionDescriptor<TestdataSimpleBigDecimalScoreSolution> buildSolutionDescriptor() {
        return TestdataSimpleBigDecimalScoreSolution.buildSolutionDescriptor();
    }
}
