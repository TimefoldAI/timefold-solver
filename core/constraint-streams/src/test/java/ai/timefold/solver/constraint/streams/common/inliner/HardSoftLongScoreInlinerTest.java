package ai.timefold.solver.constraint.streams.common.inliner;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.score.TestdataHardSoftLongScoreSolution;

import org.junit.jupiter.api.Test;

class HardSoftLongScoreInlinerTest extends AbstractScoreInlinerTest<TestdataHardSoftLongScoreSolution, HardSoftLongScore> {

    @Test
    void defaultScore() {
        HardSoftLongScoreInliner scoreInliner =
                new HardSoftLongScoreInliner(constraintMatchEnabled);
        assertThat(scoreInliner.extractScore(0)).isEqualTo(HardSoftLongScore.ZERO);
    }

    @Test
    void impactHard() {
        HardSoftLongScoreInliner scoreInliner =
                new HardSoftLongScoreInliner(constraintMatchEnabled);

        HardSoftLongScore constraintWeight = HardSoftLongScore.ofHard(90);
        WeightedScoreImpacter<HardSoftLongScore, HardSoftLongScoreContext> hardImpacter =
                scoreInliner.buildWeightedScoreImpacter(buildConstraint(constraintWeight), constraintWeight);
        UndoScoreImpacter undo1 = hardImpacter.impactScore(1, JustificationsSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftLongScore.of(90, 0));

        UndoScoreImpacter undo2 = hardImpacter.impactScore(2, JustificationsSupplier.empty());
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
        HardSoftLongScoreInliner scoreInliner =
                new HardSoftLongScoreInliner(constraintMatchEnabled);

        HardSoftLongScore constraintWeight = HardSoftLongScore.ofSoft(90);
        WeightedScoreImpacter<HardSoftLongScore, HardSoftLongScoreContext> hardImpacter =
                scoreInliner.buildWeightedScoreImpacter(buildConstraint(constraintWeight), constraintWeight);
        UndoScoreImpacter undo1 = hardImpacter.impactScore(1, JustificationsSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftLongScore.of(0, 90));

        UndoScoreImpacter undo2 = hardImpacter.impactScore(2, JustificationsSupplier.empty());
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
        HardSoftLongScoreInliner scoreInliner =
                new HardSoftLongScoreInliner(constraintMatchEnabled);

        HardSoftLongScore constraintWeight = HardSoftLongScore.of(10, 100);
        WeightedScoreImpacter<HardSoftLongScore, HardSoftLongScoreContext> hardImpacter =
                scoreInliner.buildWeightedScoreImpacter(buildConstraint(constraintWeight), constraintWeight);
        UndoScoreImpacter undo1 = hardImpacter.impactScore(10, JustificationsSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardSoftLongScore.of(100, 1_000));

        UndoScoreImpacter undo2 = hardImpacter.impactScore(20, JustificationsSupplier.empty());
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
}
