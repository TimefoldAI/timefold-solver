package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;

final class HardMediumSoftScoreContext
        extends ScoreContext<HardMediumSoftScore, HardMediumSoftScoreInliner> {

    public HardMediumSoftScoreContext(HardMediumSoftScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            HardMediumSoftScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public ScoreImpact<HardMediumSoftScore> changeSoftScoreBy(long matchWeight,
            ConstraintMatchSupplier<HardMediumSoftScore> constraintMatchSupplier) {
        var softImpact = Math.multiplyExact(constraintWeight.softScore(), matchWeight);
        inliner.softScore = Math.addExact(inliner.softScore, softImpact);
        var scoreImpact = new SoftImpact(inliner, softImpact);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<HardMediumSoftScore> changeMediumScoreBy(long matchWeight,
            ConstraintMatchSupplier<HardMediumSoftScore> constraintMatchSupplier) {
        var mediumImpact = Math.multiplyExact(constraintWeight.mediumScore(), matchWeight);
        inliner.mediumScore = Math.addExact(inliner.mediumScore, mediumImpact);
        var scoreImpact = new MediumImpact(inliner, mediumImpact);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<HardMediumSoftScore> changeHardScoreBy(long matchWeight,
            ConstraintMatchSupplier<HardMediumSoftScore> constraintMatchSupplier) {
        var hardImpact = Math.multiplyExact(constraintWeight.hardScore(), matchWeight);
        inliner.hardScore = Math.addExact(inliner.hardScore, hardImpact);
        var scoreImpact = new HardImpact(inliner, hardImpact);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<HardMediumSoftScore> changeScoreBy(long matchWeight,
            ConstraintMatchSupplier<HardMediumSoftScore> constraintMatchSupplier) {
        var hardImpact = Math.multiplyExact(constraintWeight.hardScore(), matchWeight);
        var mediumImpact = Math.multiplyExact(constraintWeight.mediumScore(), matchWeight);
        var softImpact = Math.multiplyExact(constraintWeight.softScore(), matchWeight);
        inliner.hardScore = Math.addExact(inliner.hardScore, hardImpact);
        inliner.mediumScore = Math.addExact(inliner.mediumScore, mediumImpact);
        inliner.softScore = Math.addExact(inliner.softScore, softImpact);
        var scoreImpact = new ComplexImpact(inliner, hardImpact, mediumImpact, softImpact);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    @NullMarked
    private record SoftImpact(HardMediumSoftScoreInliner inliner,
            long softImpact) implements ScoreImpact<HardMediumSoftScore> {

        @Override
        public void undo() {
            inliner.softScore = Math.subtractExact(inliner.softScore, softImpact);
        }

        @Override
        public HardMediumSoftScore toScore() {
            return HardMediumSoftScore.ofSoft(softImpact);
        }

    }

    @NullMarked
    private record MediumImpact(HardMediumSoftScoreInliner inliner,
            long mediumImpact) implements ScoreImpact<HardMediumSoftScore> {

        @Override
        public void undo() {
            inliner.mediumScore = Math.subtractExact(inliner.mediumScore, mediumImpact);
        }

        @Override
        public HardMediumSoftScore toScore() {
            return HardMediumSoftScore.ofMedium(mediumImpact);
        }

    }

    @NullMarked
    private record HardImpact(HardMediumSoftScoreInliner inliner,
            long hardImpact) implements ScoreImpact<HardMediumSoftScore> {

        @Override
        public void undo() {
            inliner.hardScore = Math.subtractExact(inliner.hardScore, hardImpact);
        }

        @Override
        public HardMediumSoftScore toScore() {
            return HardMediumSoftScore.ofHard(hardImpact);
        }

    }

    @NullMarked
    private record ComplexImpact(HardMediumSoftScoreInliner inliner, long hardImpact, long mediumImpact,
            long softImpact) implements ScoreImpact<HardMediumSoftScore> {

        @Override
        public void undo() {
            inliner.hardScore = Math.subtractExact(inliner.hardScore, hardImpact);
            inliner.mediumScore = Math.subtractExact(inliner.mediumScore, mediumImpact);
            inliner.softScore = Math.subtractExact(inliner.softScore, softImpact);
        }

        @Override
        public HardMediumSoftScore toScore() {
            return HardMediumSoftScore.of(hardImpact, mediumImpact, softImpact);
        }

    }

}
