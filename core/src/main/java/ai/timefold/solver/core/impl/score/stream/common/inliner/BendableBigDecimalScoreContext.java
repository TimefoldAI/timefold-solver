package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;

final class BendableBigDecimalScoreContext extends ScoreContext<BendableBigDecimalScore, BendableBigDecimalScoreInliner> {

    private final int hardScoreLevelCount;
    private final int softScoreLevelCount;
    private final int scoreLevel;
    private final BigDecimal scoreLevelWeight;

    public BendableBigDecimalScoreContext(BendableBigDecimalScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            BendableBigDecimalScore constraintWeight, int hardScoreLevelCount, int softScoreLevelCount, int scoreLevel,
            BigDecimal scoreLevelWeight) {
        super(parent, constraint, constraintWeight);
        this.hardScoreLevelCount = hardScoreLevelCount;
        this.softScoreLevelCount = softScoreLevelCount;
        this.scoreLevel = scoreLevel;
        this.scoreLevelWeight = scoreLevelWeight;
    }

    public BendableBigDecimalScoreContext(BendableBigDecimalScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            BendableBigDecimalScore constraintWeight, int hardScoreLevelCount, int softScoreLevelCount) {
        this(parent, constraint, constraintWeight, hardScoreLevelCount, softScoreLevelCount, -1, BigDecimal.ZERO);
    }

    public ScoreImpact<BendableBigDecimalScore> changeSoftScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<BendableBigDecimalScore> constraintMatchSupplier) {
        var softImpact = scoreLevelWeight.multiply(matchWeight);
        parent.softScores[scoreLevel] = parent.softScores[scoreLevel].add(softImpact);
        var scoreImpact =
                new SingleSoftLevelScoreImpact(parent, hardScoreLevelCount, softScoreLevelCount, scoreLevel, softImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<BendableBigDecimalScore> changeHardScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<BendableBigDecimalScore> constraintMatchSupplier) {
        var hardImpact = scoreLevelWeight.multiply(matchWeight);
        parent.hardScores[scoreLevel] = parent.hardScores[scoreLevel].add(hardImpact);
        var scoreImpact =
                new SingleHardLevelScoreImpact(parent, hardScoreLevelCount, softScoreLevelCount, scoreLevel, hardImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<BendableBigDecimalScore> changeScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<BendableBigDecimalScore> constraintMatchSupplier) {
        var hardImpacts = new BigDecimal[hardScoreLevelCount];
        var softImpacts = new BigDecimal[softScoreLevelCount];
        for (var hardScoreLevel = 0; hardScoreLevel < hardScoreLevelCount; hardScoreLevel++) {
            var hardImpact = constraintWeight.hardScore(hardScoreLevel).multiply(matchWeight);
            hardImpacts[hardScoreLevel] = hardImpact;
            parent.hardScores[hardScoreLevel] = parent.hardScores[hardScoreLevel].add(hardImpact);
        }
        for (var softScoreLevel = 0; softScoreLevel < softScoreLevelCount; softScoreLevel++) {
            var softImpact = constraintWeight.softScore(softScoreLevel).multiply(matchWeight);
            softImpacts[softScoreLevel] = softImpact;
            parent.softScores[softScoreLevel] = parent.softScores[softScoreLevel].add(softImpact);
        }
        var scoreImpact =
                new ComplexScoreImpact(parent, hardScoreLevelCount, softScoreLevelCount, hardImpacts, softImpacts);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    @NullMarked
    private record SingleSoftLevelScoreImpact(BendableBigDecimalScoreInliner inliner, int hardScoreLevelCount,
            int softScoreLevelCount, int scoreLevel, BigDecimal impact)
            implements
                ScoreImpact<BendableBigDecimalScore> {

        @Override
        public AbstractScoreInliner<BendableBigDecimalScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.softScores[scoreLevel] = inliner.softScores[scoreLevel].subtract(impact);
        }

        @Override
        public BendableBigDecimalScore toScore() {
            return BendableBigDecimalScore.ofSoft(hardScoreLevelCount, softScoreLevelCount, scoreLevel, impact);
        }
    }

    @NullMarked
    private record SingleHardLevelScoreImpact(BendableBigDecimalScoreInliner inliner, int hardScoreLevelCount,
            int softScoreLevelCount, int scoreLevel, BigDecimal impact)
            implements
                ScoreImpact<BendableBigDecimalScore> {

        @Override
        public AbstractScoreInliner<BendableBigDecimalScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.hardScores[scoreLevel] = inliner.hardScores[scoreLevel].subtract(impact);
        }

        @Override
        public BendableBigDecimalScore toScore() {
            return BendableBigDecimalScore.ofHard(hardScoreLevelCount, softScoreLevelCount, scoreLevel, impact);
        }
    }

    @NullMarked
    private record ComplexScoreImpact(BendableBigDecimalScoreInliner inliner, int hardScoreLevelCount, int softScoreLevelCount,
            BigDecimal[] hardImpacts, BigDecimal[] softImpacts)
            implements
                ScoreImpact<BendableBigDecimalScore> {

        @Override
        public AbstractScoreInliner<BendableBigDecimalScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            for (var hardScoreLevel = 0; hardScoreLevel < hardScoreLevelCount; hardScoreLevel++) {
                inliner.hardScores[hardScoreLevel] = inliner.hardScores[hardScoreLevel].subtract(hardImpacts[hardScoreLevel]);
            }
            for (var softScoreLevel = 0; softScoreLevel < softScoreLevelCount; softScoreLevel++) {
                inliner.softScores[softScoreLevel] = inliner.softScores[softScoreLevel].subtract(softImpacts[softScoreLevel]);
            }
        }

        @Override
        public BendableBigDecimalScore toScore() {
            return BendableBigDecimalScore.of(hardImpacts, softImpacts);
        }
    }

}
