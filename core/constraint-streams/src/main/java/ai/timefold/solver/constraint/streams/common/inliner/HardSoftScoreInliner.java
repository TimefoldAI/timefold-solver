package ai.timefold.solver.constraint.streams.common.inliner;

import ai.timefold.solver.constraint.streams.common.AbstractConstraint;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;

final class HardSoftScoreInliner extends AbstractScoreInliner<HardSoftScore> {

    int hardScore;
    int softScore;

    HardSoftScoreInliner(boolean constraintMatchEnabled) {
        this(1, constraintMatchEnabled);
    }

    HardSoftScoreInliner(int constraintCount, boolean constraintMatchEnabled) {
        super(constraintCount, constraintMatchEnabled);
    }

    @Override
    public WeightedScoreImpacter<HardSoftScoreContext> buildWeightedScoreImpacter(
            AbstractConstraint<?, ?, ?> constraint,
            HardSoftScore constraintWeight) {
        validateConstraintWeight(constraint, constraintWeight);
        HardSoftScoreContext context = new HardSoftScoreContext(this, constraint, constraintWeight);
        if (constraintWeight.softScore() == 0) {
            return WeightedScoreImpacter.of(context, HardSoftScoreContext::changeHardScoreBy);
        } else if (constraintWeight.hardScore() == 0) {
            return WeightedScoreImpacter.of(context, HardSoftScoreContext::changeSoftScoreBy);
        } else {
            return WeightedScoreImpacter.of(context, HardSoftScoreContext::changeScoreBy);
        }
    }

    @Override
    public HardSoftScore extractScore(int initScore) {
        return HardSoftScore.ofUninitialized(initScore, hardScore, softScore);
    }

    @Override
    public String toString() {
        return HardSoftScore.class.getSimpleName() + " inliner";
    }

}
