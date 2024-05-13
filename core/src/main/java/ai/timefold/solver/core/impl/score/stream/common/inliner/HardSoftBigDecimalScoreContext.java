package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.core.impl.score.ScoreUtil;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

final class HardSoftBigDecimalScoreContext extends ScoreContext<HardSoftBigDecimalScore, HardSoftBigDecimalScoreInliner> {

    public HardSoftBigDecimalScoreContext(HardSoftBigDecimalScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            HardSoftBigDecimalScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public UndoScoreImpacter changeSoftScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<HardSoftBigDecimalScore> constraintMatchSupplier) {
        var softImpact = ScoreUtil.multiply(constraintWeight.softScore(), matchWeight);
        parent.softScore = parent.softScore.add(softImpact);
        UndoScoreImpacter undoScoreImpact = () -> parent.softScore = parent.softScore.subtract(softImpact);
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardSoftBigDecimalScore.ofSoft(softImpact), constraintMatchSupplier);
    }

    public UndoScoreImpacter changeHardScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<HardSoftBigDecimalScore> constraintMatchSupplier) {
        var hardImpact = ScoreUtil.multiply(constraintWeight.hardScore(), matchWeight);
        parent.hardScore = parent.hardScore.add(hardImpact);
        UndoScoreImpacter undoScoreImpact = () -> parent.hardScore = parent.hardScore.subtract(hardImpact);
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardSoftBigDecimalScore.ofHard(hardImpact), constraintMatchSupplier);
    }

    public UndoScoreImpacter changeScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<HardSoftBigDecimalScore> constraintMatchSupplier) {
        var hardImpact = ScoreUtil.multiply(constraintWeight.hardScore(), matchWeight);
        var softImpact = ScoreUtil.multiply(constraintWeight.softScore(), matchWeight);
        parent.hardScore = parent.hardScore.add(hardImpact);
        parent.softScore = parent.softScore.add(softImpact);
        UndoScoreImpacter undoScoreImpact = () -> {
            parent.hardScore = parent.hardScore.subtract(hardImpact);
            parent.softScore = parent.softScore.subtract(softImpact);
        };
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardSoftBigDecimalScore.of(hardImpact, softImpact),
                constraintMatchSupplier);
    }

}
