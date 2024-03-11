package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

final class HardSoftScoreContext extends ScoreContext<HardSoftScore, HardSoftScoreInliner> {

    public HardSoftScoreContext(HardSoftScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            HardSoftScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public UndoScoreImpacter changeSoftScoreBy(int matchWeight,
            ConstraintMatchSupplier<HardSoftScore> constraintMatchSupplier) {
        int softImpact = constraintWeight.softScore() * matchWeight;
        parent.softScore += softImpact;
        UndoScoreImpacter undoScoreImpact = () -> parent.softScore -= softImpact;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardSoftScore.ofSoft(softImpact), constraintMatchSupplier);
    }

    public UndoScoreImpacter changeHardScoreBy(int matchWeight,
            ConstraintMatchSupplier<HardSoftScore> constraintMatchSupplier) {
        int hardImpact = constraintWeight.hardScore() * matchWeight;
        parent.hardScore += hardImpact;
        UndoScoreImpacter undoScoreImpact = () -> parent.hardScore -= hardImpact;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardSoftScore.ofHard(hardImpact), constraintMatchSupplier);
    }

    public UndoScoreImpacter changeScoreBy(int matchWeight, ConstraintMatchSupplier<HardSoftScore> constraintMatchSupplier) {
        int hardImpact = constraintWeight.hardScore() * matchWeight;
        int softImpact = constraintWeight.softScore() * matchWeight;
        parent.hardScore += hardImpact;
        parent.softScore += softImpact;
        UndoScoreImpacter undoScoreImpact = () -> {
            parent.hardScore -= hardImpact;
            parent.softScore -= softImpact;
        };
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardSoftScore.of(hardImpact, softImpact), constraintMatchSupplier);
    }

}
