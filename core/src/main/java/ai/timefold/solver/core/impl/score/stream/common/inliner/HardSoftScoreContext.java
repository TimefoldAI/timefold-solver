package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;

final class HardSoftScoreContext extends ScoreContext<HardSoftScore, HardSoftScoreInliner> {

    public HardSoftScoreContext(HardSoftScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            HardSoftScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public ScoreImpact<HardSoftScore> changeSoftScoreBy(int matchWeight,
            ConstraintMatchSupplier<HardSoftScore> constraintMatchSupplier) {
        var softImpact = Math.multiplyExact(constraintWeight.softScore(), matchWeight);
        inliner.softScore = Math.addExact(inliner.softScore, softImpact);
        var scoreImpact = new SoftImpact(inliner, softImpact);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<HardSoftScore> changeHardScoreBy(int matchWeight,
            ConstraintMatchSupplier<HardSoftScore> constraintMatchSupplier) {
        var hardImpact = Math.multiplyExact(constraintWeight.hardScore(), matchWeight);
        inliner.hardScore = Math.addExact(inliner.hardScore, hardImpact);
        var scoreImpact = new HardImpact(inliner, hardImpact);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<HardSoftScore> changeScoreBy(int matchWeight,
            ConstraintMatchSupplier<HardSoftScore> constraintMatchSupplier) {
        var hardImpact = Math.multiplyExact(constraintWeight.hardScore(), matchWeight);
        var softImpact = Math.multiplyExact(constraintWeight.softScore(), matchWeight);
        inliner.hardScore = Math.addExact(inliner.hardScore, hardImpact);
        inliner.softScore = Math.addExact(inliner.softScore, softImpact);
        var scoreImpact = new ComplexImpact(inliner, hardImpact, softImpact);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    @NullMarked
    private record SoftImpact(HardSoftScoreInliner inliner, int softImpact) implements ScoreImpact<HardSoftScore> {

        @Override
        public void undo() {
            inliner.softScore = Math.subtractExact(inliner.softScore, softImpact);
        }

        @Override
        public HardSoftScore toScore() {
            return HardSoftScore.ofSoft(softImpact);
        }

    }

    @NullMarked
    private record HardImpact(HardSoftScoreInliner inliner, int hardImpact) implements ScoreImpact<HardSoftScore> {

        @Override
        public void undo() {
            inliner.hardScore = Math.subtractExact(inliner.hardScore, hardImpact);
        }

        @Override
        public HardSoftScore toScore() {
            return HardSoftScore.ofHard(hardImpact);
        }

    }

    @NullMarked
    private record ComplexImpact(HardSoftScoreInliner inliner, int hardImpact,
            int softImpact) implements ScoreImpact<HardSoftScore> {

        @Override
        public void undo() {
            inliner.hardScore = Math.subtractExact(inliner.hardScore, hardImpact);
            inliner.softScore = Math.subtractExact(inliner.softScore, softImpact);
        }

        @Override
        public HardSoftScore toScore() {
            return HardSoftScore.of(hardImpact, softImpact);
        }

    }

}
