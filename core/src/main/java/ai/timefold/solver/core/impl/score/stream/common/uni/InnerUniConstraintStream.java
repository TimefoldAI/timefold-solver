package ai.timefold.solver.core.impl.score.stream.common.uni;

import static ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics.STANDARD;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintBuilder;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.core.impl.bavet.bi.joiner.BiJoinerComber;
import ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;

import org.jspecify.annotations.NonNull;

public interface InnerUniConstraintStream<A> extends UniConstraintStream<A> {

    static <A> BiFunction<A, Score<?>, DefaultConstraintJustification> createDefaultJustificationMapping() {
        return (a, score) -> DefaultConstraintJustification.of(score, a);
    }

    static <A> Function<A, Collection<?>> createDefaultIndictedObjectsMapping() {
        return Collections::singletonList;
    }

    RetrievalSemantics getRetrievalSemantics();

    /**
     * This method returns true if the constraint stream is guaranteed to only produce distinct tuples.
     * See {@link #distinct()} for details.
     *
     * @return true if the guarantee of distinct tuples is provided
     */
    boolean guaranteesDistinct();

    @Override
    default @NonNull <B> BiConstraintStream<A, B> join(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B>... joiners) {
        if (getRetrievalSemantics() == STANDARD) {
            return join(getConstraintFactory().forEach(otherClass), joiners);
        } else {
            return join(getConstraintFactory().from(otherClass), joiners);
        }
    }

    /**
     * Allows {@link ConstraintFactory#forEachUniquePair(Class)} to reuse the joiner combing logic.
     *
     * @param otherStream never null
     * @param joinerComber never null
     * @param <B>
     * @return never null
     */
    <B> BiConstraintStream<A, B> join(UniConstraintStream<B> otherStream, BiJoinerComber<A, B> joinerComber);

    @Override
    default @NonNull <B> UniConstraintStream<A> ifExists(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B>... joiners) {
        if (getRetrievalSemantics() == STANDARD) {
            return ifExists(getConstraintFactory().forEach(otherClass), joiners);
        } else {
            // Calls fromUnfiltered() for backward compatibility only
            return ifExists(getConstraintFactory().fromUnfiltered(otherClass), joiners);
        }
    }

    @Override
    default @NonNull <B> UniConstraintStream<A> ifExistsIncludingUnassigned(@NonNull Class<B> otherClass,
            @NonNull BiJoiner<A, B>... joiners) {
        if (getRetrievalSemantics() == STANDARD) {
            return ifExists(getConstraintFactory().forEachIncludingUnassigned(otherClass), joiners);
        } else {
            return ifExists(getConstraintFactory().fromUnfiltered(otherClass), joiners);
        }
    }

    @Override
    default @NonNull <B> UniConstraintStream<A> ifNotExists(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B>... joiners) {
        if (getRetrievalSemantics() == STANDARD) {
            return ifNotExists(getConstraintFactory().forEach(otherClass), joiners);
        } else {
            // Calls fromUnfiltered() for backward compatibility only
            return ifNotExists(getConstraintFactory().fromUnfiltered(otherClass), joiners);
        }
    }

    @Override
    default @NonNull <B> UniConstraintStream<A> ifNotExistsIncludingUnassigned(@NonNull Class<B> otherClass,
            @NonNull BiJoiner<A, B>... joiners) {
        if (getRetrievalSemantics() == STANDARD) {
            return ifNotExists(getConstraintFactory().forEachIncludingUnassigned(otherClass), joiners);
        } else {
            return ifNotExists(getConstraintFactory().fromUnfiltered(otherClass), joiners);
        }
    }

    @Override
    default @NonNull UniConstraintStream<A> distinct() {
        if (guaranteesDistinct()) {
            return this;
        } else {
            return groupBy(ConstantLambdaUtils.identity());
        }
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> penalize(@NonNull Score_ constraintWeight,
            @NonNull ToIntFunction<A> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> penalizeLong(
            @NonNull Score_ constraintWeight,
            @NonNull ToLongFunction<A> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> penalizeBigDecimal(
            @NonNull Score_ constraintWeight,
            @NonNull Function<A, BigDecimal> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default UniConstraintBuilder<A, ?> penalizeConfigurable(ToIntFunction<A> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default UniConstraintBuilder<A, ?> penalizeConfigurableLong(ToLongFunction<A> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default UniConstraintBuilder<A, ?> penalizeConfigurableBigDecimal(Function<A, BigDecimal> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> reward(@NonNull Score_ constraintWeight,
            @NonNull ToIntFunction<A> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> rewardLong(@NonNull Score_ constraintWeight,
            @NonNull ToLongFunction<A> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> rewardBigDecimal(
            @NonNull Score_ constraintWeight,
            @NonNull Function<A, BigDecimal> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default UniConstraintBuilder<A, ?> rewardConfigurable(ToIntFunction<A> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default UniConstraintBuilder<A, ?> rewardConfigurableLong(ToLongFunction<A> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default UniConstraintBuilder<A, ?> rewardConfigurableBigDecimal(Function<A, BigDecimal> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> impact(@NonNull Score_ constraintWeight,
            @NonNull ToIntFunction<A> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> impactLong(@NonNull Score_ constraintWeight,
            @NonNull ToLongFunction<A> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> impactBigDecimal(
            @NonNull Score_ constraintWeight,
            @NonNull Function<A, BigDecimal> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default UniConstraintBuilder<A, ?> impactConfigurable(ToIntFunction<A> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default UniConstraintBuilder<A, ?> impactConfigurableLong(ToLongFunction<A> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default UniConstraintBuilder<A, ?> impactConfigurableBigDecimal(Function<A, BigDecimal> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.MIXED);
    }

    <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> innerImpact(Score_ constraintWeight,
            ToIntFunction<A> matchWeigher, ScoreImpactType scoreImpactType);

    <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> innerImpact(Score_ constraintWeight,
            ToLongFunction<A> matchWeigher, ScoreImpactType scoreImpactType);

    <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> innerImpact(Score_ constraintWeight,
            Function<A, BigDecimal> matchWeigher, ScoreImpactType scoreImpactType);

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
