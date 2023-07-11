package ai.timefold.solver.constraint.streams.bavet.uni;

import java.math.BigDecimal;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraint;
import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.common.BavetScoringConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.NodeBuildHelper;
import ai.timefold.solver.constraint.streams.common.inliner.JustificationsSupplier;
import ai.timefold.solver.constraint.streams.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.constraint.streams.common.inliner.WeightedScoreImpacter;
import ai.timefold.solver.core.api.score.Score;

final class BavetScoringUniConstraintStream<Solution_, A>
        extends BavetAbstractUniConstraintStream<Solution_, A>
        implements BavetScoringConstraintStream<Solution_> {

    private final ToIntFunction<A> intMatchWeigher;
    private final ToLongFunction<A> longMatchWeigher;
    private final Function<A, BigDecimal> bigDecimalMatchWeigher;
    private BavetConstraint<Solution_> constraint;

    public BavetScoringUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractUniConstraintStream<Solution_, A> parent, ToIntFunction<A> intMatchWeigher) {
        this(constraintFactory, parent, intMatchWeigher, null, null);
        if (intMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    public BavetScoringUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractUniConstraintStream<Solution_, A> parent, ToLongFunction<A> longMatchWeigher) {
        this(constraintFactory, parent, null, longMatchWeigher, null);
        if (longMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    public BavetScoringUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractUniConstraintStream<Solution_, A> parent, Function<A, BigDecimal> bigDecimalMatchWeigher) {
        this(constraintFactory, parent, null, null, bigDecimalMatchWeigher);
        if (bigDecimalMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    private BavetScoringUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractUniConstraintStream<Solution_, A> parent, ToIntFunction<A> intMatchWeigher,
            ToLongFunction<A> longMatchWeigher, Function<A, BigDecimal> bigDecimalMatchWeigher) {
        super(constraintFactory, parent);
        this.intMatchWeigher = intMatchWeigher;
        this.longMatchWeigher = longMatchWeigher;
        this.bigDecimalMatchWeigher = bigDecimalMatchWeigher;
    }

    @Override
    public void setConstraint(BavetConstraint<Solution_> constraint) {
        this.constraint = constraint;
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper) {
        assertEmptyChildStreamList();
        var constraintMatchEnabled = buildHelper.getScoreInliner().isConstraintMatchEnabled();
        var scoreImpacter = constraintMatchEnabled ? buildScoreImpacterWithConstraintMatch() : buildScoreImpacter();
        var constraintWeight = buildHelper.getConstraintWeight(constraint);
        var weightedScoreImpacter = buildHelper.getScoreInliner().buildWeightedScoreImpacter(constraint, constraintWeight);
        var scorer = new UniScorer<>(weightedScoreImpacter, scoreImpacter,
                buildHelper.reserveTupleStoreIndex(parent.getTupleSource()));
        buildHelper.putInsertUpdateRetract(this, scorer);
    }

    private BiFunction<WeightedScoreImpacter<?>, A, UndoScoreImpacter> buildScoreImpacter() {
        if (intMatchWeigher != null) {
            return (impacter, a) -> {
                int matchWeight = intMatchWeigher.applyAsInt(a);
                return impacter.impactScore(matchWeight, null);
            };
        } else if (longMatchWeigher != null) {
            return (impacter, a) -> {
                long matchWeight = longMatchWeigher.applyAsLong(a);
                return impacter.impactScore(matchWeight, null);
            };
        } else if (bigDecimalMatchWeigher != null) {
            return (impacter, a) -> {
                BigDecimal matchWeight = bigDecimalMatchWeigher.apply(a);
                return impacter.impactScore(matchWeight, null);
            };
        } else {
            throw new IllegalStateException("Impossible state: neither of the supported match weighers provided.");
        }
    }

    private BiFunction<WeightedScoreImpacter<?>, A, UndoScoreImpacter> buildScoreImpacterWithConstraintMatch() {
        if (intMatchWeigher != null) {
            return (impacter, a) -> {
                int matchWeight = intMatchWeigher.applyAsInt(a);
                return impactWithConstraintMatch(impacter, matchWeight, a);
            };
        } else if (longMatchWeigher != null) {
            return (impacter, a) -> {
                long matchWeight = longMatchWeigher.applyAsLong(a);
                return impactWithConstraintMatch(impacter, matchWeight, a);
            };
        } else if (bigDecimalMatchWeigher != null) {
            return (impacter, a) -> {
                BigDecimal matchWeight = bigDecimalMatchWeigher.apply(a);
                return impactWithConstraintMatch(impacter, matchWeight, a);
            };
        } else {
            throw new IllegalStateException("Impossible state: neither of the supported match weighers provided.");
        }
    }

    private static <A> UndoScoreImpacter impactWithConstraintMatch(WeightedScoreImpacter<?> impacter, int matchWeight, A a) {
        var constraint = impacter.getContext().getConstraint();
        var justificationsSupplier = JustificationsSupplier.of(constraint, constraint.getJustificationMapping(),
                constraint.getIndictedObjectsMapping(), a);
        return impacter.impactScore(matchWeight, justificationsSupplier);
    }

    private static <A> UndoScoreImpacter impactWithConstraintMatch(WeightedScoreImpacter<?> impacter, long matchWeight, A a) {
        var constraint = impacter.getContext().getConstraint();
        var justificationsSupplier = JustificationsSupplier.of(constraint, constraint.getJustificationMapping(),
                constraint.getIndictedObjectsMapping(), a);
        return impacter.impactScore(matchWeight, justificationsSupplier);
    }

    private static <A> UndoScoreImpacter impactWithConstraintMatch(WeightedScoreImpacter<?> impacter, BigDecimal matchWeight,
            A a) {
        var constraint = impacter.getContext().getConstraint();
        var justificationsSupplier = JustificationsSupplier.of(constraint, constraint.getJustificationMapping(),
                constraint.getIndictedObjectsMapping(), a);
        return impacter.impactScore(matchWeight, justificationsSupplier);
    }

    // ************************************************************************
    // Equality for node sharing
    // ************************************************************************

    // No node sharing

    @Override
    public String toString() {
        return "Scoring(" + constraint.getConstraintName() + ")";
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

}
