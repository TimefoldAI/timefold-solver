package ai.timefold.solver.constraint.streams.bavet.quad;

import static ai.timefold.solver.constraint.streams.common.inliner.JustificationsSupplier.of;

import java.math.BigDecimal;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraint;
import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.common.BavetScoringConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.NodeBuildHelper;
import ai.timefold.solver.constraint.streams.common.inliner.JustificationsSupplier;
import ai.timefold.solver.constraint.streams.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.constraint.streams.common.inliner.WeightedScoreImpacter;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.ToIntQuadFunction;
import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.api.score.Score;

final class BavetScoringQuadConstraintStream<Solution_, A, B, C, D>
        extends BavetAbstractQuadConstraintStream<Solution_, A, B, C, D>
        implements BavetScoringConstraintStream<Solution_> {

    private final boolean noMatchWeigher;
    private final ToIntQuadFunction<A, B, C, D> intMatchWeigher;
    private final ToLongQuadFunction<A, B, C, D> longMatchWeigher;
    private final QuadFunction<A, B, C, D, BigDecimal> bigDecimalMatchWeigher;
    private BavetConstraint<Solution_> constraint;

    public BavetScoringQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> parent,
            ToIntQuadFunction<A, B, C, D> intMatchWeigher) {
        this(constraintFactory, parent, false, intMatchWeigher, null, null);
        if (intMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    public BavetScoringQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> parent,
            ToLongQuadFunction<A, B, C, D> longMatchWeigher) {
        this(constraintFactory, parent, false, null, longMatchWeigher, null);
        if (longMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    public BavetScoringQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> parent,
            QuadFunction<A, B, C, D, BigDecimal> bigDecimalMatchWeigher) {
        this(constraintFactory, parent, false, null, null, bigDecimalMatchWeigher);
        if (bigDecimalMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    private BavetScoringQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> parent,
            boolean noMatchWeigher,
            ToIntQuadFunction<A, B, C, D> intMatchWeigher, ToLongQuadFunction<A, B, C, D> longMatchWeigher,
            QuadFunction<A, B, C, D, BigDecimal> bigDecimalMatchWeigher) {
        super(constraintFactory, parent);
        this.noMatchWeigher = noMatchWeigher;
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
        Score_ constraintWeight = buildHelper.getConstraintWeight(constraint);
        WeightedScoreImpacter<Score_, ?> weightedScoreImpacter =
                buildHelper.getScoreInliner().buildWeightedScoreImpacter(constraint, constraintWeight);
        boolean constraintMatchEnabled = buildHelper.getScoreInliner().isConstraintMatchEnabled();
        QuadFunction<A, B, C, D, UndoScoreImpacter> scoreImpacter;
        if (intMatchWeigher != null) {
            if (constraintMatchEnabled) {
                scoreImpacter = (a, b, c, d) -> {
                    int matchWeight = intMatchWeigher.applyAsInt(a, b, c, d);
                    constraint.assertCorrectImpact(matchWeight);
                    JustificationsSupplier justificationsSupplier =
                            of(constraint, constraint.getJustificationMapping(), constraint.getIndictedObjectsMapping(), a, b,
                                    c, d);
                    return weightedScoreImpacter.impactScore(matchWeight, justificationsSupplier);
                };
            } else {
                scoreImpacter = (a, b, c, d) -> {
                    int matchWeight = intMatchWeigher.applyAsInt(a, b, c, d);
                    constraint.assertCorrectImpact(matchWeight);
                    return weightedScoreImpacter.impactScore(matchWeight, null);
                };
            }
        } else if (longMatchWeigher != null) {
            if (constraintMatchEnabled) {
                scoreImpacter = (a, b, c, d) -> {
                    long matchWeight = longMatchWeigher.applyAsLong(a, b, c, d);
                    constraint.assertCorrectImpact(matchWeight);
                    JustificationsSupplier justificationsSupplier =
                            of(constraint, constraint.getJustificationMapping(), constraint.getIndictedObjectsMapping(), a, b,
                                    c, d);
                    return weightedScoreImpacter.impactScore(matchWeight, justificationsSupplier);
                };
            } else {
                scoreImpacter = (a, b, c, d) -> {
                    long matchWeight = longMatchWeigher.applyAsLong(a, b, c, d);
                    constraint.assertCorrectImpact(matchWeight);
                    return weightedScoreImpacter.impactScore(matchWeight, null);
                };
            }
        } else if (bigDecimalMatchWeigher != null) {
            if (constraintMatchEnabled) {
                scoreImpacter = (a, b, c, d) -> {
                    BigDecimal matchWeight = bigDecimalMatchWeigher.apply(a, b, c, d);
                    constraint.assertCorrectImpact(matchWeight);
                    JustificationsSupplier justificationsSupplier =
                            of(constraint, constraint.getJustificationMapping(), constraint.getIndictedObjectsMapping(), a, b,
                                    c, d);
                    return weightedScoreImpacter.impactScore(matchWeight, justificationsSupplier);
                };
            } else {
                scoreImpacter = (a, b, c, d) -> {
                    BigDecimal matchWeight = bigDecimalMatchWeigher.apply(a, b, c, d);
                    constraint.assertCorrectImpact(matchWeight);
                    return weightedScoreImpacter.impactScore(matchWeight, null);
                };
            }
        } else if (noMatchWeigher) {
            if (constraintMatchEnabled) {
                scoreImpacter = (a, b, c, d) -> {
                    JustificationsSupplier justificationsSupplier =
                            of(constraint, constraint.getJustificationMapping(), constraint.getIndictedObjectsMapping(), a, b,
                                    c, d);
                    return weightedScoreImpacter.impactScore(1, justificationsSupplier);
                };
            } else {
                scoreImpacter = (a, b, c, d) -> weightedScoreImpacter.impactScore(1, null);
            }
        } else {
            throw new IllegalStateException("Impossible state: neither of the supported match weighers provided.");
        }
        QuadScorer<A, B, C, D> scorer = new QuadScorer<>(constraint.getConstraintPackage(), constraint.getConstraintName(),
                constraintWeight, scoreImpacter, buildHelper.reserveTupleStoreIndex(parent.getTupleSource()));
        buildHelper.putInsertUpdateRetract(this, scorer);
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
