package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.util.Arrays;
import java.util.Map;

import ai.timefold.solver.core.api.score.BendableScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

public final class BendableScoreInliner extends AbstractScoreInliner<BendableScore> {

    final long[] hardScores;
    final long[] softScores;

    BendableScoreInliner(Map<Constraint, BendableScore> constraintWeightMap,
            ConstraintMatchPolicy constraintMatchPolicy, int hardLevelsSize, int softLevelsSize) {
        super(constraintWeightMap, constraintMatchPolicy);
        hardScores = new long[hardLevelsSize];
        softScores = new long[softLevelsSize];
    }

    @Override
    public WeightedScoreImpacter<BendableScore, ?> buildWeightedScoreImpacter(AbstractConstraint<?, ?, ?> constraint) {
        Integer singleLevel = null;
        var constraintWeight = constraintWeightMap.get(constraint);
        for (var i = 0; i < constraintWeight.levelsSize(); i++) {
            if (constraintWeight.hardOrSoftScore(i) != 0L) {
                if (singleLevel != null) {
                    singleLevel = null;
                    break;
                }
                singleLevel = i;
            }
        }
        if (singleLevel != null) {
            var isHardScore = singleLevel < constraintWeight.hardLevelsSize();
            var level = isHardScore ? singleLevel : singleLevel - constraintWeight.hardLevelsSize();
            var context = new BendableScoreContext(this, constraint, constraintWeight,
                    hardScores.length, softScores.length, level, constraintWeight.hardOrSoftScore(singleLevel));
            if (isHardScore) {
                return WeightedScoreImpacter.of(context, BendableScoreContext::changeHardScoreBy);
            } else {
                return WeightedScoreImpacter.of(context, BendableScoreContext::changeSoftScoreBy);
            }
        } else {
            var context =
                    new BendableScoreContext(this, constraint, constraintWeight, hardScores.length, softScores.length);
            return WeightedScoreImpacter.of(context, BendableScoreContext::changeScoreBy);
        }
    }

    @Override
    public BendableScore extractScore() {
        return BendableScore.of(Arrays.copyOf(hardScores, hardScores.length), Arrays.copyOf(softScores, softScores.length));
    }

    @Override
    public String toString() {
        return BendableScore.class.getSimpleName() + " inliner";
    }

}
