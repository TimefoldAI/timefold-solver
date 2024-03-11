package ai.timefold.solver.core.impl.score.stream.common.inliner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.score.TestdataHardMediumSoftLongScoreSolution;

import org.junit.jupiter.api.Test;

class HardMediumSoftLongScoreInlinerTest
        extends AbstractScoreInlinerTest<TestdataHardMediumSoftLongScoreSolution, HardMediumSoftLongScore> {

    @Test
    void defaultScore() {
        var scoreInliner = buildScoreInliner(Collections.emptyMap(), constraintMatchEnabled);
        assertThat(scoreInliner.extractScore(0)).isEqualTo(HardMediumSoftLongScore.ZERO);
    }

    @Test
    void impactHard() {
        var constraintWeight = HardMediumSoftLongScore.ofHard(90);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardMediumSoftLongScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(1, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftLongScore.of(90, 0, 0));

        var undo2 = impacter.impactScore(2, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftLongScore.of(270, 0, 0));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftLongScore.of(90, 0, 0));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftLongScore.of(0, 0, 0));
    }

    @Test
    void impactMedium() {
        var constraintWeight = HardMediumSoftLongScore.ofMedium(90);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardMediumSoftLongScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(1, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftLongScore.of(0, 90, 0));

        var undo2 = impacter.impactScore(2, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftLongScore.of(0, 270, 0));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftLongScore.of(0, 90, 0));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftLongScore.of(0, 0, 0));
    }

    @Test
    void impactSoft() {
        var constraintWeight = HardMediumSoftLongScore.ofSoft(90);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardMediumSoftLongScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(1, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftLongScore.of(0, 0, 90));

        var undo2 = impacter.impactScore(2, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftLongScore.of(0, 0, 270));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftLongScore.of(0, 0, 90));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftLongScore.of(0, 0, 0));
    }

    @Test
    void impactAll() {
        var constraintWeight = HardMediumSoftLongScore.of(10, 100, 1_000);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardMediumSoftLongScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(10, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftLongScore.of(100, 1_000, 10_000));

        var undo2 = impacter.impactScore(20, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftLongScore.of(300, 3_000, 30_000));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftLongScore.of(100, 1_000, 10_000));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftLongScore.of(0, 0, 0));
    }

    @Override
    protected SolutionDescriptor<TestdataHardMediumSoftLongScoreSolution> buildSolutionDescriptor() {
        return TestdataHardMediumSoftLongScoreSolution.buildSolutionDescriptor();
    }

    @Override
    protected AbstractScoreInliner<HardMediumSoftLongScore>
            buildScoreInliner(Map<Constraint, HardMediumSoftLongScore> constraintWeightMap, boolean constraintMatchEnabled) {
        return new HardMediumSoftLongScoreInliner(constraintWeightMap, constraintMatchEnabled);
    }
}
