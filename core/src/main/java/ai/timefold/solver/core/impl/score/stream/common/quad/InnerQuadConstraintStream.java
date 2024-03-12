package ai.timefold.solver.core.impl.score.stream.common.quad;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.ToIntQuadFunction;
import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintBuilder;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;

public interface InnerQuadConstraintStream<A, B, C, D> extends QuadConstraintStream<A, B, C, D> {

    static <A, B, C, D> PentaFunction<A, B, C, D, Score<?>, DefaultConstraintJustification>
            createDefaultJustificationMapping() {
        return (a, b, c, d, score) -> DefaultConstraintJustification.of(score, a, b, c, d);
    }

    static <A, B, C, D> QuadFunction<A, B, C, D, Collection<?>> createDefaultIndictedObjectsMapping() {
        return Arrays::asList;
    }

    @Override
    default <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> penalize(Score_ constraintWeight,
            ToIntQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> penalizeLong(Score_ constraintWeight,
            ToLongQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> penalizeBigDecimal(Score_ constraintWeight,
            QuadFunction<A, B, C, D, BigDecimal> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default QuadConstraintBuilder<A, B, C, D, ?> penalizeConfigurable(ToIntQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default QuadConstraintBuilder<A, B, C, D, ?> penalizeConfigurableLong(ToLongQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default QuadConstraintBuilder<A, B, C, D, ?>
            penalizeConfigurableBigDecimal(QuadFunction<A, B, C, D, BigDecimal> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> reward(Score_ constraintWeight,
            ToIntQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> rewardLong(Score_ constraintWeight,
            ToLongQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> rewardBigDecimal(Score_ constraintWeight,
            QuadFunction<A, B, C, D, BigDecimal> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default QuadConstraintBuilder<A, B, C, D, ?> rewardConfigurable(ToIntQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default QuadConstraintBuilder<A, B, C, D, ?> rewardConfigurableLong(ToLongQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default QuadConstraintBuilder<A, B, C, D, ?>
            rewardConfigurableBigDecimal(QuadFunction<A, B, C, D, BigDecimal> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> impact(Score_ constraintWeight,
            ToIntQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> impactLong(Score_ constraintWeight,
            ToLongQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> impactBigDecimal(Score_ constraintWeight,
            QuadFunction<A, B, C, D, BigDecimal> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default QuadConstraintBuilder<A, B, C, D, ?> impactConfigurable(ToIntQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default QuadConstraintBuilder<A, B, C, D, ?> impactConfigurableLong(ToLongQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default QuadConstraintBuilder<A, B, C, D, ?>
            impactConfigurableBigDecimal(QuadFunction<A, B, C, D, BigDecimal> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.MIXED);
    }

    <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> innerImpact(Score_ constraintWeight,
            ToIntQuadFunction<A, B, C, D> matchWeigher, ScoreImpactType scoreImpactType);

    <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> innerImpact(Score_ constraintWeight,
            ToLongQuadFunction<A, B, C, D> matchWeigher, ScoreImpactType scoreImpactType);

    <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> innerImpact(Score_ constraintWeight,
            QuadFunction<A, B, C, D, BigDecimal> matchWeigher, ScoreImpactType scoreImpactType);

    @Override
    default Constraint penalize(String constraintName, Score<?> constraintWeight) {
        return penalize((Score) constraintWeight)
                .asConstraint(constraintName);
    }

    @Override
    default Constraint penalize(String constraintPackage, String constraintName, Score<?> constraintWeight) {
        return penalize((Score) constraintWeight)
                .asConstraint(constraintPackage, constraintName);
    }

    @Override
    default Constraint penalizeConfigurable(String constraintName) {
        return penalizeConfigurable()
                .asConstraint(constraintName);
    }

    @Override
    default Constraint penalizeConfigurable(String constraintPackage, String constraintName) {
        return penalizeConfigurable()
                .asConstraint(constraintPackage, constraintName);
    }

    @Override
    default Constraint reward(String constraintName, Score<?> constraintWeight) {
        return reward((Score) constraintWeight)
                .asConstraint(constraintName);
    }

    @Override
    default Constraint reward(String constraintPackage, String constraintName, Score<?> constraintWeight) {
        return reward((Score) constraintWeight)
                .asConstraint(constraintPackage, constraintName);
    }

    @Override
    default Constraint rewardConfigurable(String constraintName) {
        return rewardConfigurable()
                .asConstraint(constraintName);
    }

    @Override
    default Constraint rewardConfigurable(String constraintPackage, String constraintName) {
        return penalizeConfigurable()
                .asConstraint(constraintPackage, constraintName);
    }

    @Override
    default Constraint impact(String constraintName, Score<?> constraintWeight) {
        return impact((Score) constraintWeight)
                .asConstraint(constraintName);
    }

    @Override
    default Constraint impact(String constraintPackage, String constraintName, Score<?> constraintWeight) {
        return impact((Score) constraintWeight)
                .asConstraint(constraintPackage, constraintName);
    }

}
