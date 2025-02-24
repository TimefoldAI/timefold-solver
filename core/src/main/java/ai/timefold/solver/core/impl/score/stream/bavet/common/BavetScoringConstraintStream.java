package ai.timefold.solver.core.impl.score.stream.bavet.common;

import java.math.BigDecimal;
import java.util.Set;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraint;
import ai.timefold.solver.core.impl.score.stream.common.inliner.ConstraintMatchSupplier;
import ai.timefold.solver.core.impl.score.stream.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.core.impl.score.stream.common.inliner.WeightedScoreImpacter;

public interface BavetScoringConstraintStream<Solution_> {

    void setConstraint(BavetConstraint<Solution_> constraint);

    void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet);

    static <Score_ extends Score<Score_>> UndoScoreImpacter
            impactWithConstraintMatchNoJustifications(WeightedScoreImpacter<Score_, ?> impacter, int matchWeight) {
        var constraintMatchSupplier = ConstraintMatchSupplier.<Score_> empty();
        return impacter.impactScore(matchWeight, constraintMatchSupplier);
    }

    static <Score_ extends Score<Score_>> UndoScoreImpacter
            impactWithConstraintMatchNoJustifications(WeightedScoreImpacter<Score_, ?> impacter, long matchWeight) {
        var constraintMatchSupplier = ConstraintMatchSupplier.<Score_> empty();
        return impacter.impactScore(matchWeight, constraintMatchSupplier);
    }

    static <Score_ extends Score<Score_>> UndoScoreImpacter
            impactWithConstraintMatchNoJustifications(WeightedScoreImpacter<Score_, ?> impacter, BigDecimal matchWeight) {
        var constraintMatchSupplier = ConstraintMatchSupplier.<Score_> empty();
        return impacter.impactScore(matchWeight, constraintMatchSupplier);
    }

}
