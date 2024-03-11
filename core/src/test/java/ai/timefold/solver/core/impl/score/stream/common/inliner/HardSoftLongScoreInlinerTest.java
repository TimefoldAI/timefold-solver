package ai.timefold.solver.core.impl.score.stream.common.inliner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.score.TestdataHardSoftLongScoreSolution;

import org.junit.jupiter.api.Test;

class HardSoftLongScoreInlinerTest extends AbstractScoreInlinerTest<TestdataHardSoftLongScoreSolution, HardSoftLongScore> {

    @Test
    void defaultScore() {
        var scoreInliner = buildScoreInliner(Collections.emptyMap(), constraintMatchEnabled);
        assertThat(scoreInliner.extractScore(0)).isEqualTo(HardSoftLongScore.ZERO);
    }

    @Test
    void impactHard() {
        var constraintWeight = HardSoftLongScore.ofHard(90);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardSoftLongScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(1, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftLongScore.of(90, 0));

        var undo2 = impacter.impactScore(2, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftLongScore.of(270, 0));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftLongScore.of(90, 0));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftLongScore.of(0, 0));
    }

    @Test
    void impactSoft() {
        var constraintWeight = HardSoftLongScore.ofSoft(90);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardSoftLongScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(1, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftLongScore.of(0, 90));

        var undo2 = impacter.impactScore(2, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftLongScore.of(0, 270));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftLongScore.of(0, 90));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftLongScore.of(0, 0));
    }

    @Test
    void impactAll() {
        var constraintWeight = HardSoftLongScore.of(10, 100);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardSoftLongScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(10, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftLongScore.of(100, 1_000));

        var undo2 = impacter.impactScore(20, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftLongScore.of(300, 3_000));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftLongScore.of(100, 1_000));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftLongScore.of(0, 0));
    }

    @Override
    protected SolutionDescriptor<TestdataHardSoftLongScoreSolution> buildSolutionDescriptor() {
        return TestdataHardSoftLongScoreSolution.buildSolutionDescriptor();
    }

    @Override
    protected AbstractScoreInliner<HardSoftLongScore> buildScoreInliner(Map<Constraint, HardSoftLongScore> constraintWeightMap,
            boolean constraintMatchEnabled) {
        return new HardSoftLongScoreInliner(constraintWeightMap, constraintMatchEnabled);
    }
}
