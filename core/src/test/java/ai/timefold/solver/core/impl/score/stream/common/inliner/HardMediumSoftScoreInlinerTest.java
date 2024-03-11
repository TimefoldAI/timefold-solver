package ai.timefold.solver.core.impl.score.stream.common.inliner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.score.TestdataHardMediumSoftScoreSolution;

import org.junit.jupiter.api.Test;

class HardMediumSoftScoreInlinerTest
        extends AbstractScoreInlinerTest<TestdataHardMediumSoftScoreSolution, HardMediumSoftScore> {

    @Test
    void defaultScore() {
        var scoreInliner = buildScoreInliner(Collections.emptyMap(), constraintMatchEnabled);
        assertThat(scoreInliner.extractScore(0)).isEqualTo(HardMediumSoftScore.ZERO);
    }

    @Test
    void impactHard() {
        var constraintWeight = HardMediumSoftScore.ofHard(90);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardMediumSoftScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(1, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftScore.of(90, 0, 0));

        var undo2 = impacter.impactScore(2, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftScore.of(270, 0, 0));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftScore.of(90, 0, 0));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftScore.of(0, 0, 0));
    }

    @Test
    void impactMedium() {
        var constraintWeight = HardMediumSoftScore.ofMedium(90);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardMediumSoftScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(1, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftScore.of(0, 90, 0));

        var undo2 = impacter.impactScore(2, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftScore.of(0, 270, 0));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftScore.of(0, 90, 0));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftScore.of(0, 0, 0));
    }

    @Test
    void impactSoft() {
        var constraintWeight = HardMediumSoftScore.ofSoft(90);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardMediumSoftScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(1, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftScore.of(0, 0, 90));

        var undo2 = impacter.impactScore(2, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftScore.of(0, 0, 270));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftScore.of(0, 0, 90));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftScore.of(0, 0, 0));
    }

    @Test
    void impactAll() {
        var constraintWeight = HardMediumSoftScore.of(10, 100, 1_000);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<HardMediumSoftScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(10, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftScore.of(100, 1_000, 10_000));

        var undo2 = impacter.impactScore(20, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftScore.of(300, 3_000, 30_000));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftScore.of(100, 1_000, 10_000));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftScore.of(0, 0, 0));
    }

    @Override
    protected SolutionDescriptor<TestdataHardMediumSoftScoreSolution> buildSolutionDescriptor() {
        return TestdataHardMediumSoftScoreSolution.buildSolutionDescriptor();
    }

    @Override
    protected AbstractScoreInliner<HardMediumSoftScore>
            buildScoreInliner(Map<Constraint, HardMediumSoftScore> constraintWeightMap, boolean constraintMatchEnabled) {
        return new HardMediumSoftScoreInliner(constraintWeightMap, constraintMatchEnabled);
    }
}
