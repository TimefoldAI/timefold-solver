package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

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

    public UndoScoreImpacter changeSoftScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<BendableBigDecimalScore> constraintMatchSupplier) {
        BigDecimal softImpact = scoreLevelWeight.multiply(matchWeight);
        parent.softScores[scoreLevel] = parent.softScores[scoreLevel].add(softImpact);
        UndoScoreImpacter undoScoreImpact =
                () -> parent.softScores[scoreLevel] = parent.softScores[scoreLevel].subtract(softImpact);
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact,
                BendableBigDecimalScore.ofSoft(hardScoreLevelCount, softScoreLevelCount, scoreLevel, softImpact),
                constraintMatchSupplier);
    }

    public UndoScoreImpacter changeHardScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<BendableBigDecimalScore> constraintMatchSupplier) {
        BigDecimal hardImpact = scoreLevelWeight.multiply(matchWeight);
        parent.hardScores[scoreLevel] = parent.hardScores[scoreLevel].add(hardImpact);
        UndoScoreImpacter undoScoreImpact =
                () -> parent.hardScores[scoreLevel] = parent.hardScores[scoreLevel].subtract(hardImpact);
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact,
                BendableBigDecimalScore.ofHard(hardScoreLevelCount, softScoreLevelCount, scoreLevel, hardImpact),
                constraintMatchSupplier);
    }

    public UndoScoreImpacter changeScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<BendableBigDecimalScore> constraintMatchSupplier) {
        BigDecimal[] hardImpacts = new BigDecimal[hardScoreLevelCount];
        BigDecimal[] softImpacts = new BigDecimal[softScoreLevelCount];
        for (int hardScoreLevel = 0; hardScoreLevel < hardScoreLevelCount; hardScoreLevel++) {
            BigDecimal hardImpact = constraintWeight.hardScore(hardScoreLevel).multiply(matchWeight);
            hardImpacts[hardScoreLevel] = hardImpact;
            parent.hardScores[hardScoreLevel] = parent.hardScores[hardScoreLevel].add(hardImpact);
        }
        for (int softScoreLevel = 0; softScoreLevel < softScoreLevelCount; softScoreLevel++) {
            BigDecimal softImpact = constraintWeight.softScore(softScoreLevel).multiply(matchWeight);
            softImpacts[softScoreLevel] = softImpact;
            parent.softScores[softScoreLevel] = parent.softScores[softScoreLevel].add(softImpact);
        }
        UndoScoreImpacter undoScoreImpact = () -> {
            for (int hardScoreLevel = 0; hardScoreLevel < hardScoreLevelCount; hardScoreLevel++) {
                parent.hardScores[hardScoreLevel] = parent.hardScores[hardScoreLevel].subtract(hardImpacts[hardScoreLevel]);
            }
            for (int softScoreLevel = 0; softScoreLevel < softScoreLevelCount; softScoreLevel++) {
                parent.softScores[softScoreLevel] = parent.softScores[softScoreLevel].subtract(softImpacts[softScoreLevel]);
            }
        };
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, BendableBigDecimalScore.of(hardImpacts, softImpacts),
                constraintMatchSupplier);
    }

}
