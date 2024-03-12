package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

final class HardMediumSoftScoreInliner extends AbstractScoreInliner<HardMediumSoftScore> {

    int hardScore;
    int mediumScore;
    int softScore;

    HardMediumSoftScoreInliner(Map<Constraint, HardMediumSoftScore> constraintWeightMap, boolean constraintMatchEnabled) {
        super(constraintWeightMap, constraintMatchEnabled);
    }

    @Override
    public WeightedScoreImpacter<HardMediumSoftScore, ?>
            buildWeightedScoreImpacter(AbstractConstraint<?, ?, ?> constraint) {
        HardMediumSoftScore constraintWeight = constraintWeightMap.get(constraint);
        int hardConstraintWeight = constraintWeight.hardScore();
        int mediumConstraintWeight = constraintWeight.mediumScore();
        int softConstraintWeight = constraintWeight.softScore();
        HardMediumSoftScoreContext context = new HardMediumSoftScoreContext(this, constraint, constraintWeight);
        if (mediumConstraintWeight == 0 && softConstraintWeight == 0) {
            return WeightedScoreImpacter.of(context, HardMediumSoftScoreContext::changeHardScoreBy);
        } else if (hardConstraintWeight == 0 && softConstraintWeight == 0) {
            return WeightedScoreImpacter.of(context, HardMediumSoftScoreContext::changeMediumScoreBy);
        } else if (hardConstraintWeight == 0 && mediumConstraintWeight == 0) {
            return WeightedScoreImpacter.of(context, HardMediumSoftScoreContext::changeSoftScoreBy);
        } else {
            return WeightedScoreImpacter.of(context, HardMediumSoftScoreContext::changeScoreBy);
        }
    }

    @Override
    public HardMediumSoftScore extractScore(int initScore) {
        return HardMediumSoftScore.ofUninitialized(initScore, hardScore, mediumScore, softScore);
    }

    @Override
    public String toString() {
        return HardMediumSoftScore.class.getSimpleName() + " inliner";
    }

}
