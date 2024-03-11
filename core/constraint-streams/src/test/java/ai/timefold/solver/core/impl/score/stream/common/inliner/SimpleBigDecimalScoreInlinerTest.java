package ai.timefold.solver.core.impl.score.stream.common.inliner;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.score.TestdataSimpleBigDecimalScoreSolution;

import org.junit.jupiter.api.Test;

class SimpleBigDecimalScoreInlinerTest
        extends AbstractScoreInlinerTest<TestdataSimpleBigDecimalScoreSolution, SimpleBigDecimalScore> {

    @Test
    void defaultScore() {
        var scoreInliner = buildScoreInliner(Collections.emptyMap(), constraintMatchEnabled);
        assertThat(scoreInliner.extractScore(0)).isEqualTo(SimpleBigDecimalScore.ZERO);
    }

    @Test
    void impact() {
        var constraintWeight = SimpleBigDecimalScore.of(BigDecimal.valueOf(10));
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<SimpleBigDecimalScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(BigDecimal.TEN, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(100)));

        var undo2 = impacter.impactScore(BigDecimal.valueOf(20), ConstraintMatchSupplier.empty());
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

    @Override
    protected AbstractScoreInliner<SimpleBigDecimalScore>
            buildScoreInliner(Map<Constraint, SimpleBigDecimalScore> constraintWeightMap, boolean constraintMatchEnabled) {
        return new SimpleBigDecimalScoreInliner(constraintWeightMap, constraintMatchEnabled);
    }
}
