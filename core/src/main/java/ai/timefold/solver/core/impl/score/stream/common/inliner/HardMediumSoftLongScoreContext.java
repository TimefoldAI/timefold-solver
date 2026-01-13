package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;

final class HardMediumSoftLongScoreContext
        extends ScoreContext<HardMediumSoftLongScore, HardMediumSoftLongScoreInliner> {

    public HardMediumSoftLongScoreContext(HardMediumSoftLongScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            HardMediumSoftLongScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public ScoreImpact<HardMediumSoftLongScore> changeSoftScoreBy(long matchWeight,
            ConstraintMatchSupplier<HardMediumSoftLongScore> constraintMatchSupplier) {
        var softImpact = Math.multiplyExact(constraintWeight.softScore(), matchWeight);
        inliner.softScore = Math.addExact(inliner.softScore, softImpact);
        var scoreImpact = new SoftImpact(inliner, softImpact);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<HardMediumSoftLongScore> changeMediumScoreBy(long matchWeight,
            ConstraintMatchSupplier<HardMediumSoftLongScore> constraintMatchSupplier) {
        var mediumImpact = Math.multiplyExact(constraintWeight.mediumScore(), matchWeight);
        inliner.mediumScore = Math.addExact(inliner.mediumScore, mediumImpact);
        var scoreImpact = new MediumImpact(inliner, mediumImpact);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<HardMediumSoftLongScore> changeHardScoreBy(long matchWeight,
            ConstraintMatchSupplier<HardMediumSoftLongScore> constraintMatchSupplier) {
        var hardImpact = Math.multiplyExact(constraintWeight.hardScore(), matchWeight);
        inliner.hardScore = Math.addExact(inliner.hardScore, hardImpact);
        var scoreImpact = new HardImpact(inliner, hardImpact);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<HardMediumSoftLongScore> changeScoreBy(long matchWeight,
            ConstraintMatchSupplier<HardMediumSoftLongScore> constraintMatchSupplier) {
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
    private record SoftImpact(HardMediumSoftLongScoreInliner inliner,
            long softImpact) implements ScoreImpact<HardMediumSoftLongScore> {

        @Override
        public void undo() {
            inliner.softScore = Math.subtractExact(inliner.softScore, softImpact);
        }

        @Override
        public HardMediumSoftLongScore toScore() {
            return HardMediumSoftLongScore.ofSoft(softImpact);
        }

    }

    @NullMarked
    private record MediumImpact(HardMediumSoftLongScoreInliner inliner,
            long mediumImpact) implements ScoreImpact<HardMediumSoftLongScore> {

        @Override
        public void undo() {
            inliner.mediumScore = Math.subtractExact(inliner.mediumScore, mediumImpact);
        }

        @Override
        public HardMediumSoftLongScore toScore() {
            return HardMediumSoftLongScore.ofMedium(mediumImpact);
        }

    }

    @NullMarked
    private record HardImpact(HardMediumSoftLongScoreInliner inliner,
            long hardImpact) implements ScoreImpact<HardMediumSoftLongScore> {

        @Override
        public void undo() {
            inliner.hardScore = Math.subtractExact(inliner.hardScore, hardImpact);
        }

        @Override
        public HardMediumSoftLongScore toScore() {
            return HardMediumSoftLongScore.ofHard(hardImpact);
        }

    }

    @NullMarked
    private record ComplexImpact(HardMediumSoftLongScoreInliner inliner, long hardImpact, long mediumImpact,
            long softImpact) implements ScoreImpact<HardMediumSoftLongScore> {

        @Override
        public void undo() {
            inliner.hardScore = Math.subtractExact(inliner.hardScore, hardImpact);
            inliner.mediumScore = Math.subtractExact(inliner.mediumScore, mediumImpact);
            inliner.softScore = Math.subtractExact(inliner.softScore, softImpact);
        }

        @Override
        public HardMediumSoftLongScore toScore() {
            return HardMediumSoftLongScore.of(hardImpact, mediumImpact, softImpact);
        }

    }

}
