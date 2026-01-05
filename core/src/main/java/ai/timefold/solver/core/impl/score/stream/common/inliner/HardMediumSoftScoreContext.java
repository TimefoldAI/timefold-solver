package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;

final class HardMediumSoftScoreContext extends ScoreContext<HardMediumSoftScore, HardMediumSoftScoreInliner> {

    public HardMediumSoftScoreContext(HardMediumSoftScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            HardMediumSoftScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public ScoreImpact<HardMediumSoftScore> changeSoftScoreBy(int matchWeight,
            ConstraintMatchSupplier<HardMediumSoftScore> constraintMatchSupplier) {
        var softImpact = constraintWeight.softScore() * matchWeight;
        inliner.softScore += softImpact;
        var scoreImpact = new SoftImpact(inliner, softImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<HardMediumSoftScore> changeMediumScoreBy(int matchWeight,
            ConstraintMatchSupplier<HardMediumSoftScore> constraintMatchSupplier) {
        var mediumImpact = constraintWeight.mediumScore() * matchWeight;
        inliner.mediumScore += mediumImpact;
        var scoreImpact = new MediumImpact(inliner, mediumImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<HardMediumSoftScore> changeHardScoreBy(int matchWeight,
            ConstraintMatchSupplier<HardMediumSoftScore> constraintMatchSupplier) {
        var hardImpact = constraintWeight.hardScore() * matchWeight;
        inliner.hardScore += hardImpact;
        var scoreImpact = new HardImpact(inliner, hardImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<HardMediumSoftScore> changeScoreBy(int matchWeight,
            ConstraintMatchSupplier<HardMediumSoftScore> constraintMatchSupplier) {
        var hardImpact = constraintWeight.hardScore() * matchWeight;
        var mediumImpact = constraintWeight.mediumScore() * matchWeight;
        var softImpact = constraintWeight.softScore() * matchWeight;
        inliner.hardScore += hardImpact;
        inliner.mediumScore += mediumImpact;
        inliner.softScore += softImpact;
        var scoreImpact = new ComplexImpact(inliner, hardImpact, mediumImpact, softImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    @NullMarked
    private record SoftImpact(HardMediumSoftScoreInliner inliner,
            int softImpact) implements ScoreImpact<HardMediumSoftScore> {

        @Override
        public AbstractScoreInliner<HardMediumSoftScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.softScore -= softImpact;
        }

        @Override
        public HardMediumSoftScore toScore() {
            return HardMediumSoftScore.ofSoft(softImpact);
        }

    }

    @NullMarked
    private record MediumImpact(HardMediumSoftScoreInliner inliner,
            int mediumImpact) implements ScoreImpact<HardMediumSoftScore> {

        @Override
        public AbstractScoreInliner<HardMediumSoftScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.mediumScore -= mediumImpact;
        }

        @Override
        public HardMediumSoftScore toScore() {
            return HardMediumSoftScore.ofMedium(mediumImpact);
        }

    }

    @NullMarked
    private record HardImpact(HardMediumSoftScoreInliner inliner,
            int hardImpact) implements ScoreImpact<HardMediumSoftScore> {

        @Override
        public AbstractScoreInliner<HardMediumSoftScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.hardScore -= hardImpact;
        }

        @Override
        public HardMediumSoftScore toScore() {
            return HardMediumSoftScore.ofHard(hardImpact);
        }

    }

    @NullMarked
    private record ComplexImpact(HardMediumSoftScoreInliner inliner, int hardImpact, int mediumImpact,
            int softImpact) implements ScoreImpact<HardMediumSoftScore> {

        @Override
        public AbstractScoreInliner<HardMediumSoftScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.hardScore -= hardImpact;
            inliner.mediumScore -= mediumImpact;
            inliner.softScore -= softImpact;
        }

        @Override
        public HardMediumSoftScore toScore() {
            return HardMediumSoftScore.of(hardImpact, mediumImpact, softImpact);
        }

    }

}
