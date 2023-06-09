package ai.timefold.solver.constraint.streams.common.inliner;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;

final class HardMediumSoftScoreInliner extends AbstractScoreInliner<HardMediumSoftScore> {

    private int hardScore;
    private int mediumScore;
    private int softScore;

    HardMediumSoftScoreInliner(boolean constraintMatchEnabled) {
        super(constraintMatchEnabled);
    }

    @Override
    public WeightedScoreImpacter<HardMediumSoftScore, HardMediumSoftScoreContext> buildWeightedScoreImpacter(
            Constraint constraint, HardMediumSoftScore constraintWeight) {
        validateConstraintWeight(constraint, constraintWeight);
        int hardConstraintWeight = constraintWeight.hardScore();
        int mediumConstraintWeight = constraintWeight.mediumScore();
        int softConstraintWeight = constraintWeight.softScore();
        HardMediumSoftScoreContext context =
                new HardMediumSoftScoreContext(this, constraint, constraintWeight,
                        impact -> this.hardScore += impact, impact -> this.mediumScore += impact,
                        impact -> this.softScore += impact);
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
