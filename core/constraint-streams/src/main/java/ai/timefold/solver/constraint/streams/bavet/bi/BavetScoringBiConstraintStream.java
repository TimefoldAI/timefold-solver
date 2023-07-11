package ai.timefold.solver.constraint.streams.bavet.bi;

import static ai.timefold.solver.constraint.streams.common.inliner.JustificationsSupplier.of;

import java.math.BigDecimal;
import java.util.function.BiFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraint;
import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.common.BavetScoringConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.NodeBuildHelper;
import ai.timefold.solver.constraint.streams.common.inliner.JustificationsSupplier;
import ai.timefold.solver.constraint.streams.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.constraint.streams.common.inliner.WeightedScoreImpacter;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;

final class BavetScoringBiConstraintStream<Solution_, A, B>
        extends BavetAbstractBiConstraintStream<Solution_, A, B>
        implements BavetScoringConstraintStream<Solution_> {

    private final ToIntBiFunction<A, B> intMatchWeigher;
    private final ToLongBiFunction<A, B> longMatchWeigher;
    private final BiFunction<A, B, BigDecimal> bigDecimalMatchWeigher;
    private BavetConstraint<Solution_> constraint;

    public BavetScoringBiConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractBiConstraintStream<Solution_, A, B> parent, ToIntBiFunction<A, B> intMatchWeigher) {
        this(constraintFactory, parent, intMatchWeigher, null, null);
        if (intMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    public BavetScoringBiConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractBiConstraintStream<Solution_, A, B> parent, ToLongBiFunction<A, B> longMatchWeigher) {
        this(constraintFactory, parent, null, longMatchWeigher, null);
        if (longMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    public BavetScoringBiConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractBiConstraintStream<Solution_, A, B> parent, BiFunction<A, B, BigDecimal> bigDecimalMatchWeigher) {
        this(constraintFactory, parent, null, null, bigDecimalMatchWeigher);
        if (bigDecimalMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    private BavetScoringBiConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractBiConstraintStream<Solution_, A, B> parent, ToIntBiFunction<A, B> intMatchWeigher,
            ToLongBiFunction<A, B> longMatchWeigher, BiFunction<A, B, BigDecimal> bigDecimalMatchWeigher) {
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
        var scorer = new BiScorer<>(weightedScoreImpacter, scoreImpacter,
                buildHelper.reserveTupleStoreIndex(parent.getTupleSource()));
        buildHelper.putInsertUpdateRetract(this, scorer);
    }

    private TriFunction<WeightedScoreImpacter<?>, A, B, UndoScoreImpacter> buildScoreImpacter() {
        if (intMatchWeigher != null) {
            return (impacter, a, b) -> {
                int matchWeight = intMatchWeigher.applyAsInt(a, b);
                return impacter.impactScore(matchWeight, null);
            };
        } else if (longMatchWeigher != null) {
            return (impacter, a, b) -> {
                long matchWeight = longMatchWeigher.applyAsLong(a, b);
                return impacter.impactScore(matchWeight, null);
            };
        } else if (bigDecimalMatchWeigher != null) {
            return (impacter, a, b) -> {
                BigDecimal matchWeight = bigDecimalMatchWeigher.apply(a, b);
                return impacter.impactScore(matchWeight, null);
            };
        } else {
            throw new IllegalStateException("Impossible state: neither of the supported match weighers provided.");
        }
    }

    private TriFunction<WeightedScoreImpacter<?>, A, B, UndoScoreImpacter> buildScoreImpacterWithConstraintMatch() {
        if (intMatchWeigher != null) {
            return (impacter, a, b) -> {
                int matchWeight = intMatchWeigher.applyAsInt(a, b);
                return impactWithConstraintMatch(impacter, matchWeight, a, b);
            };
        } else if (longMatchWeigher != null) {
            return (impacter, a, b) -> {
                long matchWeight = longMatchWeigher.applyAsLong(a, b);
                return impactWithConstraintMatch(impacter, matchWeight, a, b);
            };
        } else if (bigDecimalMatchWeigher != null) {
            return (impacter, a, b) -> {
                BigDecimal matchWeight = bigDecimalMatchWeigher.apply(a, b);
                return impactWithConstraintMatch(impacter, matchWeight, a, b);
            };
        } else {
            throw new IllegalStateException("Impossible state: neither of the supported match weighers provided.");
        }
    }

    private static <A, B> UndoScoreImpacter impactWithConstraintMatch(WeightedScoreImpacter<?> impacter, int matchWeight, A a,
            B b) {
        var constraint = impacter.getContext().getConstraint();
        var justificationsSupplier = JustificationsSupplier.of(constraint, constraint.getJustificationMapping(),
                constraint.getIndictedObjectsMapping(), a, b);
        return impacter.impactScore(matchWeight, justificationsSupplier);
    }

    private static <A, B> UndoScoreImpacter impactWithConstraintMatch(WeightedScoreImpacter<?> impacter, long matchWeight, A a,
            B b) {
        var constraint = impacter.getContext().getConstraint();
        var justificationsSupplier = JustificationsSupplier.of(constraint, constraint.getJustificationMapping(),
                constraint.getIndictedObjectsMapping(), a, b);
        return impacter.impactScore(matchWeight, justificationsSupplier);
    }

    private static <A, B> UndoScoreImpacter impactWithConstraintMatch(WeightedScoreImpacter<?> impacter, BigDecimal matchWeight,
            A a, B b) {
        var constraint = impacter.getContext().getConstraint();
        var justificationsSupplier = JustificationsSupplier.of(constraint, constraint.getJustificationMapping(),
                constraint.getIndictedObjectsMapping(), a, b);
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
