package ai.timefold.solver.core.impl.score.stream.common.inliner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class SimpleScoreInlinerTest extends AbstractScoreInlinerTest<TestdataSolution, SimpleScore> {

    @Test
    void defaultScore() {
        var scoreInliner = buildScoreInliner(Collections.emptyMap(), constraintMatchEnabled);
        assertThat(scoreInliner.extractScore(0)).isEqualTo(SimpleScore.ZERO);
    }

    @Test
    void impact() {
        var constraintWeight = SimpleScore.of(10);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<SimpleScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(10, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(SimpleScore.of(100));

        var undo2 = impacter.impactScore(20, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(SimpleScore.of(300));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(SimpleScore.of(100));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(SimpleScore.of(0));
    }

    @Override
    protected SolutionDescriptor<TestdataSolution> buildSolutionDescriptor() {
        return TestdataSolution.buildSolutionDescriptor();
    }

    @Override
    protected AbstractScoreInliner<SimpleScore> buildScoreInliner(Map<Constraint, SimpleScore> constraintWeightMap,
            boolean constraintMatchEnabled) {
        return new SimpleScoreInliner(constraintWeightMap, constraintMatchEnabled);
    }

}
