package ai.timefold.solver.constraint.streams.common.inliner;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.score.TestdataSimpleLongScoreSolution;

import org.junit.jupiter.api.Test;

class SimpleLongScoreInlinerTest extends AbstractScoreInlinerTest<TestdataSimpleLongScoreSolution, SimpleLongScore> {

    @Test
    void defaultScore() {
        SimpleLongScoreInliner scoreInliner =
                new SimpleLongScoreInliner(constraintMatchEnabled);
        assertThat(scoreInliner.extractScore(0)).isEqualTo(SimpleLongScore.ZERO);
    }

    @Test
    void impact() {
        SimpleLongScoreInliner scoreInliner =
                new SimpleLongScoreInliner(constraintMatchEnabled);

        SimpleLongScore constraintWeight = SimpleLongScore.of(10);
        WeightedScoreImpacter<SimpleLongScore, SimpleLongScoreContext> hardImpacter =
                scoreInliner.buildWeightedScoreImpacter(buildConstraint(constraintWeight), constraintWeight);
        UndoScoreImpacter undo1 = hardImpacter.impactScore(10, JustificationsSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(SimpleLongScore.of(100));

        UndoScoreImpacter undo2 = hardImpacter.impactScore(20, JustificationsSupplier.empty());
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(SimpleLongScore.of(300));

        undo2.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(SimpleLongScore.of(100));

        undo1.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(SimpleLongScore.of(0));
    }

    @Override
    protected SolutionDescriptor<TestdataSimpleLongScoreSolution> buildSolutionDescriptor() {
        return TestdataSimpleLongScoreSolution.buildSolutionDescriptor();
    }
}
