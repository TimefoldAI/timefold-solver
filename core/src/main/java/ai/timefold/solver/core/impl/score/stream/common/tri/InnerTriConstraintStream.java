package ai.timefold.solver.core.impl.score.stream.common.tri;

import static ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics.STANDARD;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.ToIntTriFunction;
import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;
import ai.timefold.solver.core.api.score.stream.quad.QuadJoiner;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintBuilder;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;

import org.jspecify.annotations.NonNull;

public interface InnerTriConstraintStream<A, B, C> extends TriConstraintStream<A, B, C> {

    static <A, B, C> QuadFunction<A, B, C, Score<?>, DefaultConstraintJustification> createDefaultJustificationMapping() {
        return (a, b, c, score) -> DefaultConstraintJustification.of(score, a, b, c);
    }

    static <A, B, C> TriFunction<A, B, C, Collection<?>> createDefaultIndictedObjectsMapping() {
        return Arrays::asList;
    }

    RetrievalSemantics getRetrievalSemantics();

    /**
     * This method will return true if the constraint stream is guaranteed to only produce distinct tuples.
     * See {@link #distinct()} for details.
     *
     * @return true if the guarantee of distinct tuples is provided
     */
    boolean guaranteesDistinct();

    @Override
    default @NonNull <D> QuadConstraintStream<A, B, C, D> join(@NonNull Class<D> otherClass,
            @NonNull QuadJoiner<A, B, C, D> @NonNull... joiners) {
        if (getRetrievalSemantics() == STANDARD) {
            return join(getConstraintFactory().forEach(otherClass), joiners);
        } else {
            return join(getConstraintFactory().from(otherClass), joiners);
        }
    }

    @Override
    default @NonNull <D> TriConstraintStream<A, B, C> ifExists(@NonNull Class<D> otherClass,
            @NonNull QuadJoiner<A, B, C, D> @NonNull... joiners) {
        if (getRetrievalSemantics() == STANDARD) {
            return ifExists(getConstraintFactory().forEach(otherClass), joiners);
        } else {
            // Calls fromUnfiltered() for backward compatibility only
            return ifExists(getConstraintFactory().fromUnfiltered(otherClass), joiners);
        }
    }

    @Override
    default @NonNull <D> TriConstraintStream<A, B, C> ifExistsIncludingUnassigned(@NonNull Class<D> otherClass,
            @NonNull QuadJoiner<A, B, C, D> @NonNull... joiners) {
        if (getRetrievalSemantics() == STANDARD) {
            return ifExists(getConstraintFactory().forEachIncludingUnassigned(otherClass), joiners);
        } else {
            return ifExists(getConstraintFactory().fromUnfiltered(otherClass), joiners);
        }
    }

    @Override
    default @NonNull <D> TriConstraintStream<A, B, C> ifNotExists(@NonNull Class<D> otherClass,
            @NonNull QuadJoiner<A, B, C, D> @NonNull... joiners) {
        if (getRetrievalSemantics() == STANDARD) {
            return ifNotExists(getConstraintFactory().forEach(otherClass), joiners);
        } else {
            // Calls fromUnfiltered() for backward compatibility only
            return ifNotExists(getConstraintFactory().fromUnfiltered(otherClass), joiners);
        }
    }

    @Override
    default @NonNull <D> TriConstraintStream<A, B, C> ifNotExistsIncludingUnassigned(@NonNull Class<D> otherClass,
            @NonNull QuadJoiner<A, B, C, D> @NonNull... joiners) {
        if (getRetrievalSemantics() == STANDARD) {
            return ifNotExists(getConstraintFactory().forEachIncludingUnassigned(otherClass), joiners);
        } else {
            return ifNotExists(getConstraintFactory().fromUnfiltered(otherClass), joiners);
        }
    }

    @Override
    default @NonNull TriConstraintStream<A, B, C> distinct() {
        if (guaranteesDistinct()) {
            return this;
        } else {
            return groupBy(ConstantLambdaUtils.triPickFirst(),
                    ConstantLambdaUtils.triPickSecond(),
                    ConstantLambdaUtils.triPickThird());
        }
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> penalize(
            @NonNull Score_ constraintWeight,
            @NonNull ToIntTriFunction<A, B, C> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> penalizeLong(
            @NonNull Score_ constraintWeight,
            @NonNull ToLongTriFunction<A, B, C> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> penalizeBigDecimal(
            @NonNull Score_ constraintWeight,
            @NonNull TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default TriConstraintBuilder<A, B, C, ?> penalizeConfigurable(ToIntTriFunction<A, B, C> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default TriConstraintBuilder<A, B, C, ?> penalizeConfigurableLong(ToLongTriFunction<A, B, C> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default TriConstraintBuilder<A, B, C, ?> penalizeConfigurableBigDecimal(TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> reward(
            @NonNull Score_ constraintWeight,
            @NonNull ToIntTriFunction<A, B, C> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> rewardLong(
            @NonNull Score_ constraintWeight,
            @NonNull ToLongTriFunction<A, B, C> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> rewardBigDecimal(
            @NonNull Score_ constraintWeight,
            @NonNull TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default TriConstraintBuilder<A, B, C, ?> rewardConfigurable(ToIntTriFunction<A, B, C> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default TriConstraintBuilder<A, B, C, ?> rewardConfigurableLong(ToLongTriFunction<A, B, C> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default TriConstraintBuilder<A, B, C, ?> rewardConfigurableBigDecimal(TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> impact(
            @NonNull Score_ constraintWeight,
            @NonNull ToIntTriFunction<A, B, C> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> impactLong(
            @NonNull Score_ constraintWeight,
            @NonNull ToLongTriFunction<A, B, C> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> impactBigDecimal(
            @NonNull Score_ constraintWeight,
            @NonNull TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default TriConstraintBuilder<A, B, C, ?> impactConfigurable(ToIntTriFunction<A, B, C> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default TriConstraintBuilder<A, B, C, ?> impactConfigurableLong(ToLongTriFunction<A, B, C> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default TriConstraintBuilder<A, B, C, ?> impactConfigurableBigDecimal(TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.MIXED);
    }

    <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> innerImpact(Score_ constraintWeight,
            ToIntTriFunction<A, B, C> matchWeigher,
            ScoreImpactType scoreImpactType);

    <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> innerImpact(Score_ constraintWeight,
            ToLongTriFunction<A, B, C> matchWeigher,
            ScoreImpactType scoreImpactType);

    <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> innerImpact(Score_ constraintWeight,
            TriFunction<A, B, C, BigDecimal> matchWeigher,
            ScoreImpactType scoreImpactType);

    @Override
    default @NonNull Constraint penalize(@NonNull String constraintName, @NonNull Score<?> constraintWeight) {
        return penalize((Score) constraintWeight)
                .asConstraint(constraintName);
    }

    @Override
    default @NonNull Constraint penalize(@NonNull String constraintPackage, @NonNull String constraintName,
            @NonNull Score<?> constraintWeight) {
        return penalize((Score) constraintWeight)
                .asConstraint(constraintPackage, constraintName);
    }

    @Override
    default @NonNull Constraint penalizeConfigurable(@NonNull String constraintName) {
        return penalizeConfigurable()
                .asConstraint(constraintName);
    }

    @Override
    default @NonNull Constraint penalizeConfigurable(@NonNull String constraintPackage, @NonNull String constraintName) {
        return penalizeConfigurable()
                .asConstraint(constraintPackage, constraintName);
    }

    @Override
    default @NonNull Constraint reward(@NonNull String constraintName, @NonNull Score<?> constraintWeight) {
        return reward((Score) constraintWeight)
                .asConstraint(constraintName);
    }

    @Override
    default @NonNull Constraint reward(@NonNull String constraintPackage, @NonNull String constraintName,
            @NonNull Score<?> constraintWeight) {
        return reward((Score) constraintWeight)
                .asConstraint(constraintPackage, constraintName);
    }

    @Override
    default @NonNull Constraint rewardConfigurable(@NonNull String constraintName) {
        return rewardConfigurable()
                .asConstraint(constraintName);
    }

    @Override
    default @NonNull Constraint rewardConfigurable(@NonNull String constraintPackage, @NonNull String constraintName) {
        return penalizeConfigurable()
                .asConstraint(constraintPackage, constraintName);
    }

    @Override
    default @NonNull Constraint impact(@NonNull String constraintName, @NonNull Score<?> constraintWeight) {
        return impact((Score) constraintWeight)
                .asConstraint(constraintName);
    }

    @Override
    default @NonNull Constraint impact(@NonNull String constraintPackage, @NonNull String constraintName,
            @NonNull Score<?> constraintWeight) {
        return impact((Score) constraintWeight)
                .asConstraint(constraintPackage, constraintName);
    }

}
