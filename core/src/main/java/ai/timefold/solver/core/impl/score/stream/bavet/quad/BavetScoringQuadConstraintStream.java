package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.ToIntQuadFunction;
import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraint;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetScoringConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.common.inliner.ConstraintMatchSupplier;
import ai.timefold.solver.core.impl.score.stream.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.core.impl.score.stream.common.inliner.WeightedScoreImpacter;

final class BavetScoringQuadConstraintStream<Solution_, A, B, C, D>
        extends BavetAbstractQuadConstraintStream<Solution_, A, B, C, D>
        implements BavetScoringConstraintStream<Solution_> {

    private final ToIntQuadFunction<A, B, C, D> intMatchWeigher;
    private final ToLongQuadFunction<A, B, C, D> longMatchWeigher;
    private final QuadFunction<A, B, C, D, BigDecimal> bigDecimalMatchWeigher;
    private BavetConstraint<Solution_> constraint;

    public BavetScoringQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> parent, ToIntQuadFunction<A, B, C, D> intMatchWeigher) {
        this(constraintFactory, parent, intMatchWeigher, null, null);
        if (intMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    public BavetScoringQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> parent, ToLongQuadFunction<A, B, C, D> longMatchWeigher) {
        this(constraintFactory, parent, null, longMatchWeigher, null);
        if (longMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    public BavetScoringQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> parent,
            QuadFunction<A, B, C, D, BigDecimal> bigDecimalMatchWeigher) {
        this(constraintFactory, parent, null, null, bigDecimalMatchWeigher);
        if (bigDecimalMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    private BavetScoringQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> parent, ToIntQuadFunction<A, B, C, D> intMatchWeigher,
            ToLongQuadFunction<A, B, C, D> longMatchWeigher, QuadFunction<A, B, C, D, BigDecimal> bigDecimalMatchWeigher) {
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
        var weightedScoreImpacter = buildHelper.getScoreInliner().buildWeightedScoreImpacter(constraint);
        var scorer = new QuadScorer<>(weightedScoreImpacter, scoreImpacter,
                buildHelper.reserveTupleStoreIndex(parent.getTupleSource()));
        buildHelper.putInsertUpdateRetract(this, scorer);
    }

    private PentaFunction<WeightedScoreImpacter<?, ?>, A, B, C, D, UndoScoreImpacter> buildScoreImpacter() {
        if (intMatchWeigher != null) {
            return (impacter, a, b, c, d) -> {
                int matchWeight = intMatchWeigher.applyAsInt(a, b, c, d);
                return impacter.impactScore(matchWeight, null);
            };
        } else if (longMatchWeigher != null) {
            return (impacter, a, b, c, d) -> {
                long matchWeight = longMatchWeigher.applyAsLong(a, b, c, d);
                return impacter.impactScore(matchWeight, null);
            };
        } else if (bigDecimalMatchWeigher != null) {
            return (impacter, a, b, c, d) -> {
                BigDecimal matchWeight = bigDecimalMatchWeigher.apply(a, b, c, d);
                return impacter.impactScore(matchWeight, null);
            };
        } else {
            throw new IllegalStateException("Impossible state: neither of the supported match weighers provided.");
        }
    }

    private PentaFunction<WeightedScoreImpacter<?, ?>, A, B, C, D, UndoScoreImpacter> buildScoreImpacterWithConstraintMatch() {
        if (intMatchWeigher != null) {
            return (impacter, a, b, c, d) -> {
                int matchWeight = intMatchWeigher.applyAsInt(a, b, c, d);
                return impactWithConstraintMatch(impacter, matchWeight, a, b, c, d);
            };
        } else if (longMatchWeigher != null) {
            return (impacter, a, b, c, d) -> {
                long matchWeight = longMatchWeigher.applyAsLong(a, b, c, d);
                return impactWithConstraintMatch(impacter, matchWeight, a, b, c, d);
            };
        } else if (bigDecimalMatchWeigher != null) {
            return (impacter, a, b, c, d) -> {
                BigDecimal matchWeight = bigDecimalMatchWeigher.apply(a, b, c, d);
                return impactWithConstraintMatch(impacter, matchWeight, a, b, c, d);
            };
        } else {
            throw new IllegalStateException("Impossible state: neither of the supported match weighers provided.");
        }
    }

    private static <A, B, C, D, Score_ extends Score<Score_>> UndoScoreImpacter
            impactWithConstraintMatch(WeightedScoreImpacter<Score_, ?> impacter, int matchWeight, A a, B b, C c, D d) {
        var constraint = impacter.getContext().getConstraint();
        var constraintMatchSupplier = ConstraintMatchSupplier.<A, B, C, D, Score_> of(constraint.getJustificationMapping(),
                constraint.getIndictedObjectsMapping(), a, b, c, d);
        return impacter.impactScore(matchWeight, constraintMatchSupplier);
    }

    private static <A, B, C, D, Score_ extends Score<Score_>> UndoScoreImpacter
            impactWithConstraintMatch(WeightedScoreImpacter<Score_, ?> impacter, long matchWeight, A a, B b, C c, D d) {
        var constraint = impacter.getContext().getConstraint();
        var constraintMatchSupplier = ConstraintMatchSupplier.<A, B, C, D, Score_> of(constraint.getJustificationMapping(),
                constraint.getIndictedObjectsMapping(), a, b, c, d);
        return impacter.impactScore(matchWeight, constraintMatchSupplier);
    }

    private static <A, B, C, D, Score_ extends Score<Score_>> UndoScoreImpacter
            impactWithConstraintMatch(WeightedScoreImpacter<Score_, ?> impacter, BigDecimal matchWeight, A a, B b, C c, D d) {
        var constraint = impacter.getContext().getConstraint();
        var constraintMatchSupplier = ConstraintMatchSupplier.<A, B, C, D, Score_> of(constraint.getJustificationMapping(),
                constraint.getIndictedObjectsMapping(), a, b, c, d);
        return impacter.impactScore(matchWeight, constraintMatchSupplier);
    }

    // ************************************************************************
    // Equality for node sharing
    // ************************************************************************

    // No node sharing

    @Override
    public String toString() {
        return "Scoring(" + constraint.getConstraintRef() + ")";
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

}
