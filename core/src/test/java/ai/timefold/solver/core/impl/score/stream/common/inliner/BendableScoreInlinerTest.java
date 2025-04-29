package ai.timefold.solver.core.impl.score.stream.common.inliner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.testdomain.score.TestdataBendableScoreSolution;

import org.junit.jupiter.api.Test;

class BendableScoreInlinerTest extends AbstractScoreInlinerTest<TestdataBendableScoreSolution, BendableScore> {

    @Test
    void defaultScore() {
        var scoreInliner = buildScoreInliner(Collections.emptyMap(), constraintMatchPolicy);
        assertThat(scoreInliner.extractScore()).isEqualTo(buildScore(0, 0, 0));
    }

    @Test
    void impactHard() {
        var constraintWeight = buildScore(90, 0, 0);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<BendableScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(1, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(90, 0, 0));

        var undo2 = impacter.impactScore(2, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(270, 0, 0));

        undo2.run();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(90, 0, 0));

        undo1.run();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 0));
    }

    @Test
    void impactSoft1() {
        var constraintWeight = buildScore(0, 90, 0);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<BendableScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(1, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 90, 0));

        var undo2 = impacter.impactScore(2, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 270, 0));

        undo2.run();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 90, 0));

        undo1.run();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 0));
    }

    @Test
    void impactSoft2() {
        var constraintWeight = buildScore(0, 0, 90);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<BendableScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(1, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 90));

        var undo2 = impacter.impactScore(2, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 270));

        undo2.run();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 90));

        undo1.run();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 0));
    }

    @Test
    void impactAll() {
        var constraintWeight = buildScore(10, 100, 1_000);
        var impacter = buildScoreImpacter(constraintWeight);
        var scoreInliner = (AbstractScoreInliner<BendableScore>) impacter.getContext().parent;

        var undo1 = impacter.impactScore(10, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(100, 1_000, 10_000));

        var undo2 = impacter.impactScore(20, ConstraintMatchSupplier.empty());
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(300, 3_000, 30_000));

        undo2.run();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(100, 1_000, 10_000));

        undo1.run();
        assertThat(scoreInliner.extractScore())
                .isEqualTo(buildScore(0, 0, 0));
    }

    @Override
    protected SolutionDescriptor<TestdataBendableScoreSolution> buildSolutionDescriptor() {
        return TestdataBendableScoreSolution.buildSolutionDescriptor();
    }

    @Override
    protected AbstractScoreInliner<BendableScore> buildScoreInliner(Map<Constraint, BendableScore> constraintWeightMap,
            ConstraintMatchPolicy constraintMatchPolicy) {
        return new BendableScoreInliner(constraintWeightMap, constraintMatchPolicy, 1, 2);
    }

    private BendableScore buildScore(int hard, int soft1, int soft2) {
        return BendableScore.of(
                new int[] { hard },
                new int[] { soft1, soft2 });
    }

}
