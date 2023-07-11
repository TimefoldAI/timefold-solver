package ai.timefold.solver.constraint.streams.bavet.tri;

import java.math.BigDecimal;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraint;
import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.common.BavetScoringConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.NodeBuildHelper;
import ai.timefold.solver.constraint.streams.common.inliner.JustificationsSupplier;
import ai.timefold.solver.constraint.streams.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.constraint.streams.common.inliner.WeightedScoreImpacter;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.ToIntTriFunction;
import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;

final class BavetScoringTriConstraintStream<Solution_, A, B, C>
        extends BavetAbstractTriConstraintStream<Solution_, A, B, C>
        implements BavetScoringConstraintStream<Solution_> {

    private final ToIntTriFunction<A, B, C> intMatchWeigher;
    private final ToLongTriFunction<A, B, C> longMatchWeigher;
    private final TriFunction<A, B, C, BigDecimal> bigDecimalMatchWeigher;
    private BavetConstraint<Solution_> constraint;

    public BavetScoringTriConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractTriConstraintStream<Solution_, A, B, C> parent, ToIntTriFunction<A, B, C> intMatchWeigher) {
        this(constraintFactory, parent, intMatchWeigher, null, null);
        if (intMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    public BavetScoringTriConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractTriConstraintStream<Solution_, A, B, C> parent, ToLongTriFunction<A, B, C> longMatchWeigher) {
        this(constraintFactory, parent, null, longMatchWeigher, null);
        if (longMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    public BavetScoringTriConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractTriConstraintStream<Solution_, A, B, C> parent,
            TriFunction<A, B, C, BigDecimal> bigDecimalMatchWeigher) {
        this(constraintFactory, parent, null, null, bigDecimalMatchWeigher);
        if (bigDecimalMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    private BavetScoringTriConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractTriConstraintStream<Solution_, A, B, C> parent, ToIntTriFunction<A, B, C> intMatchWeigher,
            ToLongTriFunction<A, B, C> longMatchWeigher, TriFunction<A, B, C, BigDecimal> bigDecimalMatchWeigher) {
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
        var scorer = new TriScorer<>(weightedScoreImpacter, scoreImpacter,
                buildHelper.reserveTupleStoreIndex(parent.getTupleSource()));
        buildHelper.putInsertUpdateRetract(this, scorer);
    }

    private QuadFunction<WeightedScoreImpacter<?>, A, B, C, UndoScoreImpacter> buildScoreImpacter() {
        if (intMatchWeigher != null) {
            return (impacter, a, b, c) -> {
                int matchWeight = intMatchWeigher.applyAsInt(a, b, c);
                return impacter.impactScore(matchWeight, null);
            };
        } else if (longMatchWeigher != null) {
            return (impacter, a, b, c) -> {
                long matchWeight = longMatchWeigher.applyAsLong(a, b, c);
                return impacter.impactScore(matchWeight, null);
            };
        } else if (bigDecimalMatchWeigher != null) {
            return (impacter, a, b, c) -> {
                BigDecimal matchWeight = bigDecimalMatchWeigher.apply(a, b, c);
                return impacter.impactScore(matchWeight, null);
            };
        } else {
            throw new IllegalStateException("Impossible state: neither of the supported match weighers provided.");
        }
    }

    private QuadFunction<WeightedScoreImpacter<?>, A, B, C, UndoScoreImpacter> buildScoreImpacterWithConstraintMatch() {
        if (intMatchWeigher != null) {
            return (impacter, a, b, c) -> {
                int matchWeight = intMatchWeigher.applyAsInt(a, b, c);
                return impactWithConstraintMatch(impacter, matchWeight, a, b, c);
            };
        } else if (longMatchWeigher != null) {
            return (impacter, a, b, c) -> {
                long matchWeight = longMatchWeigher.applyAsLong(a, b, c);
                return impactWithConstraintMatch(impacter, matchWeight, a, b, c);
            };
        } else if (bigDecimalMatchWeigher != null) {
            return (impacter, a, b, c) -> {
                BigDecimal matchWeight = bigDecimalMatchWeigher.apply(a, b, c);
                return impactWithConstraintMatch(impacter, matchWeight, a, b, c);
            };
        } else {
            throw new IllegalStateException("Impossible state: neither of the supported match weighers provided.");
        }
    }

    private static <A, B, C> UndoScoreImpacter impactWithConstraintMatch(WeightedScoreImpacter<?> impacter, int matchWeight,
            A a, B b, C c) {
        var constraint = impacter.getContext().getConstraint();
        var justificationsSupplier = JustificationsSupplier.of(constraint, constraint.getJustificationMapping(),
                constraint.getIndictedObjectsMapping(), a, b, c);
        return impacter.impactScore(matchWeight, justificationsSupplier);
    }

    private static <A, B, C> UndoScoreImpacter impactWithConstraintMatch(WeightedScoreImpacter<?> impacter, long matchWeight,
            A a, B b, C c) {
        var constraint = impacter.getContext().getConstraint();
        var justificationsSupplier = JustificationsSupplier.of(constraint, constraint.getJustificationMapping(),
                constraint.getIndictedObjectsMapping(), a, b, c);
        return impacter.impactScore(matchWeight, justificationsSupplier);
    }

    private static <A, B, C> UndoScoreImpacter impactWithConstraintMatch(WeightedScoreImpacter<?> impacter,
            BigDecimal matchWeight, A a, B b, C c) {
        var constraint = impacter.getContext().getConstraint();
        var justificationsSupplier = JustificationsSupplier.of(constraint, constraint.getJustificationMapping(),
                constraint.getIndictedObjectsMapping(), a, b, c);
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
