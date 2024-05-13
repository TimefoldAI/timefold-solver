package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.impl.score.ScoreUtil;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

final class HardMediumSoftBigDecimalScoreContext
        extends ScoreContext<HardMediumSoftBigDecimalScore, HardMediumSoftBigDecimalScoreInliner> {

    public HardMediumSoftBigDecimalScoreContext(HardMediumSoftBigDecimalScoreInliner parent,
            AbstractConstraint<?, ?, ?> constraint, HardMediumSoftBigDecimalScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public UndoScoreImpacter changeSoftScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<HardMediumSoftBigDecimalScore> constraintMatchSupplier) {
        var softImpact = ScoreUtil.multiply(constraintWeight.softScore(), matchWeight);
        parent.softScore = parent.softScore.add(softImpact);
        UndoScoreImpacter undoScoreImpact = () -> parent.softScore = parent.softScore.subtract(softImpact);
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardMediumSoftBigDecimalScore.ofSoft(softImpact),
                constraintMatchSupplier);
    }

    public UndoScoreImpacter changeMediumScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<HardMediumSoftBigDecimalScore> constraintMatchSupplier) {
        var mediumImpact = ScoreUtil.multiply(constraintWeight.mediumScore(), matchWeight);
        parent.mediumScore = parent.mediumScore.add(mediumImpact);
        UndoScoreImpacter undoScoreImpact = () -> parent.mediumScore = parent.mediumScore.subtract(mediumImpact);
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardMediumSoftBigDecimalScore.ofMedium(mediumImpact),
                constraintMatchSupplier);
    }

    public UndoScoreImpacter changeHardScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<HardMediumSoftBigDecimalScore> constraintMatchSupplier) {
        var hardImpact = ScoreUtil.multiply(constraintWeight.hardScore(), matchWeight);
        parent.hardScore = parent.hardScore.add(hardImpact);
        UndoScoreImpacter undoScoreImpact = () -> parent.hardScore = parent.hardScore.subtract(hardImpact);
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardMediumSoftBigDecimalScore.ofHard(hardImpact),
                constraintMatchSupplier);
    }

    public UndoScoreImpacter changeScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<HardMediumSoftBigDecimalScore> constraintMatchSupplier) {
        var hardImpact = ScoreUtil.multiply(constraintWeight.hardScore(), matchWeight);
        var mediumImpact = ScoreUtil.multiply(constraintWeight.mediumScore(), matchWeight);
        var softImpact = ScoreUtil.multiply(constraintWeight.softScore(), matchWeight);
        parent.hardScore = parent.hardScore.add(hardImpact);
        parent.mediumScore = parent.mediumScore.add(mediumImpact);
        parent.softScore = parent.softScore.add(softImpact);
        UndoScoreImpacter undoScoreImpact = () -> {
            parent.hardScore = parent.hardScore.subtract(hardImpact);
            parent.mediumScore = parent.mediumScore.subtract(mediumImpact);
            parent.softScore = parent.softScore.subtract(softImpact);
        };
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact,
                HardMediumSoftBigDecimalScore.of(hardImpact, mediumImpact, softImpact), constraintMatchSupplier);
    }

}
