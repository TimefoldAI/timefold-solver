package ai.timefold.solver.constraint.streams.common.inliner;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.Constraint;

final class HardSoftBigDecimalScoreContext extends ScoreContext<HardSoftBigDecimalScore, HardSoftBigDecimalScoreInliner> {

    public HardSoftBigDecimalScoreContext(HardSoftBigDecimalScoreInliner parent, Constraint constraint,
            HardSoftBigDecimalScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public UndoScoreImpacter changeSoftScoreBy(BigDecimal matchWeight, JustificationsSupplier justificationsSupplier) {
        BigDecimal softImpact = constraintWeight.softScore().multiply(matchWeight);
        parent.softScore = parent.softScore.add(softImpact);
        UndoScoreImpacter undoScoreImpact = () -> parent.softScore = parent.softScore.subtract(softImpact);
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardSoftBigDecimalScore.ofSoft(softImpact), justificationsSupplier);
    }

    public UndoScoreImpacter changeHardScoreBy(BigDecimal matchWeight, JustificationsSupplier justificationsSupplier) {
        BigDecimal hardImpact = constraintWeight.hardScore().multiply(matchWeight);
        parent.hardScore = parent.hardScore.add(hardImpact);
        UndoScoreImpacter undoScoreImpact = () -> parent.hardScore = parent.hardScore.subtract(hardImpact);
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardSoftBigDecimalScore.ofHard(hardImpact), justificationsSupplier);
    }

    public UndoScoreImpacter changeScoreBy(BigDecimal matchWeight, JustificationsSupplier justificationsSupplier) {
        BigDecimal hardImpact = constraintWeight.hardScore().multiply(matchWeight);
        BigDecimal softImpact = constraintWeight.softScore().multiply(matchWeight);
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
                justificationsSupplier);
    }

}
