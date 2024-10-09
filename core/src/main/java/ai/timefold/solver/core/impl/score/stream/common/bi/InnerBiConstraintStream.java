package ai.timefold.solver.core.impl.score.stream.common.bi;

import static ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics.STANDARD;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintBuilder;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
import ai.timefold.solver.core.api.score.stream.tri.TriJoiner;
import ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;

import org.jspecify.annotations.NonNull;

public interface InnerBiConstraintStream<A, B> extends BiConstraintStream<A, B> {

    static <A, B> TriFunction<A, B, Score<?>, DefaultConstraintJustification> createDefaultJustificationMapping() {
        return (a, b, score) -> DefaultConstraintJustification.of(score, a, b);
    }

    static <A, B> BiFunction<A, B, Collection<?>> createDefaultIndictedObjectsMapping() {
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
    default <C> @NonNull TriConstraintStream<A, B, C> join(@NonNull Class<C> otherClass, TriJoiner<A, B, C>... joiners) {
        if (getRetrievalSemantics() == STANDARD) {
            return join(getConstraintFactory().forEach(otherClass), joiners);
        } else {
            return join(getConstraintFactory().from(otherClass), joiners);
        }
    }

    @Override
    default @NonNull <C> BiConstraintStream<A, B> ifExists(@NonNull Class<C> otherClass,
            TriJoiner<A, B, C> @NonNull... joiners) {
        if (getRetrievalSemantics() == STANDARD) {
            return ifExists(getConstraintFactory().forEach(otherClass), joiners);
        } else {
            // Calls fromUnfiltered() for backward compatibility only
            return ifExists(getConstraintFactory().fromUnfiltered(otherClass), joiners);
        }
    }

    @Override
    default @NonNull <C> BiConstraintStream<A, B> ifExistsIncludingUnassigned(@NonNull Class<C> otherClass,
            @NonNull TriJoiner<A, B, C> @NonNull... joiners) {
        if (getRetrievalSemantics() == STANDARD) {
            return ifExists(getConstraintFactory().forEachIncludingUnassigned(otherClass), joiners);
        } else {
            return ifExists(getConstraintFactory().fromUnfiltered(otherClass), joiners);
        }
    }

    @Override
    default <C> @NonNull BiConstraintStream<A, B> ifNotExists(@NonNull Class<C> otherClass,
            @NonNull TriJoiner<A, B, C>... joiners) {
        if (getRetrievalSemantics() == STANDARD) {
            return ifNotExists(getConstraintFactory().forEach(otherClass), joiners);
        } else {
            // Calls fromUnfiltered() for backward compatibility only
            return ifNotExists(getConstraintFactory().fromUnfiltered(otherClass), joiners);
        }
    }

    @Override
    default <C> @NonNull BiConstraintStream<A, B> ifNotExistsIncludingUnassigned(@NonNull Class<C> otherClass,
            @NonNull TriJoiner<A, B, C>... joiners) {
        if (getRetrievalSemantics() == STANDARD) {
            return ifNotExists(getConstraintFactory().forEachIncludingUnassigned(otherClass), joiners);
        } else {
            return ifNotExists(getConstraintFactory().fromUnfiltered(otherClass), joiners);
        }
    }

    @Override
    default @NonNull BiConstraintStream<A, B> distinct() {
        if (guaranteesDistinct()) {
            return this;
        } else {
            return groupBy(ConstantLambdaUtils.biPickFirst(),
                    ConstantLambdaUtils.biPickSecond());
        }
    }

    @Override
    default <Score_ extends Score<Score_>> @NonNull BiConstraintBuilder<A, B, Score_> penalize(@NonNull Score_ constraintWeight,
            @NonNull ToIntBiFunction<A, B> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> penalizeLong(
            @NonNull Score_ constraintWeight,
            @NonNull ToLongBiFunction<A, B> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> penalizeBigDecimal(
            @NonNull Score_ constraintWeight,
            @NonNull BiFunction<A, B, BigDecimal> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default BiConstraintBuilder<A, B, ?> penalizeConfigurable(ToIntBiFunction<A, B> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default BiConstraintBuilder<A, B, ?> penalizeConfigurableLong(ToLongBiFunction<A, B> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default BiConstraintBuilder<A, B, ?> penalizeConfigurableBigDecimal(BiFunction<A, B, BigDecimal> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default <Score_ extends Score<Score_>> @NonNull BiConstraintBuilder<A, B, Score_> reward(@NonNull Score_ constraintWeight,
            @NonNull ToIntBiFunction<A, B> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default <Score_ extends Score<Score_>> @NonNull BiConstraintBuilder<A, B, Score_> rewardLong(
            @NonNull Score_ constraintWeight,
            @NonNull ToLongBiFunction<A, B> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default <Score_ extends Score<Score_>> @NonNull BiConstraintBuilder<A, B, Score_> rewardBigDecimal(
            @NonNull Score_ constraintWeight,
            @NonNull BiFunction<A, B, BigDecimal> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default BiConstraintBuilder<A, B, ?> rewardConfigurable(ToIntBiFunction<A, B> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default BiConstraintBuilder<A, B, ?> rewardConfigurableLong(ToLongBiFunction<A, B> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default BiConstraintBuilder<A, B, ?> rewardConfigurableBigDecimal(BiFunction<A, B, BigDecimal> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default <Score_ extends Score<Score_>> @NonNull BiConstraintBuilder<A, B, Score_> impact(@NonNull Score_ constraintWeight,
            @NonNull ToIntBiFunction<A, B> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default <Score_ extends Score<Score_>> @NonNull BiConstraintBuilder<A, B, Score_> impactLong(
            @NonNull Score_ constraintWeight,
            @NonNull ToLongBiFunction<A, B> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default <Score_ extends Score<Score_>> @NonNull BiConstraintBuilder<A, B, Score_> impactBigDecimal(
            @NonNull Score_ constraintWeight,
            @NonNull BiFunction<A, B, BigDecimal> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default BiConstraintBuilder<A, B, ?> impactConfigurable(ToIntBiFunction<A, B> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default BiConstraintBuilder<A, B, ?> impactConfigurableLong(ToLongBiFunction<A, B> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default BiConstraintBuilder<A, B, ?> impactConfigurableBigDecimal(BiFunction<A, B, BigDecimal> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.MIXED);
    }

    <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> innerImpact(Score_ constraintWeight,
            ToIntBiFunction<A, B> matchWeigher,
            ScoreImpactType scoreImpactType);

    <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> innerImpact(Score_ constraintWeight,
            ToLongBiFunction<A, B> matchWeigher,
            ScoreImpactType scoreImpactType);

    <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> innerImpact(Score_ constraintWeight,
            BiFunction<A, B, BigDecimal> matchWeigher,
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
