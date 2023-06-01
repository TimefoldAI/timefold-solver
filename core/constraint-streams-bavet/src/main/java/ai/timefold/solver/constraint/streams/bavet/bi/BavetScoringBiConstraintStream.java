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
import ai.timefold.solver.core.api.score.Score;

final class BavetScoringBiConstraintStream<Solution_, A, B>
        extends BavetAbstractBiConstraintStream<Solution_, A, B>
        implements BavetScoringConstraintStream<Solution_> {

    private final boolean noMatchWeigher;
    private final ToIntBiFunction<A, B> intMatchWeigher;
    private final ToLongBiFunction<A, B> longMatchWeigher;
    private final BiFunction<A, B, BigDecimal> bigDecimalMatchWeigher;
    private BavetConstraint<Solution_> constraint;

    public BavetScoringBiConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractBiConstraintStream<Solution_, A, B> parent,
            ToIntBiFunction<A, B> intMatchWeigher) {
        this(constraintFactory, parent, false, intMatchWeigher, null, null);
        if (intMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    public BavetScoringBiConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractBiConstraintStream<Solution_, A, B> parent,
            ToLongBiFunction<A, B> longMatchWeigher) {
        this(constraintFactory, parent, false, null, longMatchWeigher, null);
        if (longMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    public BavetScoringBiConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractBiConstraintStream<Solution_, A, B> parent,
            BiFunction<A, B, BigDecimal> bigDecimalMatchWeigher) {
        this(constraintFactory, parent, false, null, null, bigDecimalMatchWeigher);
        if (bigDecimalMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    private BavetScoringBiConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractBiConstraintStream<Solution_, A, B> parent,
            boolean noMatchWeigher,
            ToIntBiFunction<A, B> intMatchWeigher, ToLongBiFunction<A, B> longMatchWeigher,
            BiFunction<A, B, BigDecimal> bigDecimalMatchWeigher) {
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
        BiFunction<A, B, UndoScoreImpacter> scoreImpacter;
        if (intMatchWeigher != null) {
            if (constraintMatchEnabled) {
                scoreImpacter = (a, b) -> {
                    int matchWeight = intMatchWeigher.applyAsInt(a, b);
                    constraint.assertCorrectImpact(matchWeight);
                    JustificationsSupplier justificationsSupplier =
                            of(constraint, constraint.getJustificationMapping(), constraint.getIndictedObjectsMapping(), a, b);
                    return weightedScoreImpacter.impactScore(matchWeight, justificationsSupplier);
                };
            } else {
                scoreImpacter = (a, b) -> {
                    int matchWeight = intMatchWeigher.applyAsInt(a, b);
                    constraint.assertCorrectImpact(matchWeight);
                    return weightedScoreImpacter.impactScore(matchWeight, null);
                };
            }
        } else if (longMatchWeigher != null) {
            if (constraintMatchEnabled) {
                scoreImpacter = (a, b) -> {
                    long matchWeight = longMatchWeigher.applyAsLong(a, b);
                    constraint.assertCorrectImpact(matchWeight);
                    JustificationsSupplier justificationsSupplier =
                            of(constraint, constraint.getJustificationMapping(), constraint.getIndictedObjectsMapping(), a, b);
                    return weightedScoreImpacter.impactScore(matchWeight, justificationsSupplier);
                };
            } else {
                scoreImpacter = (a, b) -> {
                    long matchWeight = longMatchWeigher.applyAsLong(a, b);
                    constraint.assertCorrectImpact(matchWeight);
                    return weightedScoreImpacter.impactScore(matchWeight, null);
                };
            }
        } else if (bigDecimalMatchWeigher != null) {
            if (constraintMatchEnabled) {
                scoreImpacter = (a, b) -> {
                    BigDecimal matchWeight = bigDecimalMatchWeigher.apply(a, b);
                    constraint.assertCorrectImpact(matchWeight);
                    JustificationsSupplier justificationsSupplier =
                            of(constraint, constraint.getJustificationMapping(), constraint.getIndictedObjectsMapping(), a, b);
                    return weightedScoreImpacter.impactScore(matchWeight, justificationsSupplier);
                };
            } else {
                scoreImpacter = (a, b) -> {
                    BigDecimal matchWeight = bigDecimalMatchWeigher.apply(a, b);
                    constraint.assertCorrectImpact(matchWeight);
                    return weightedScoreImpacter.impactScore(matchWeight, null);
                };
            }
        } else if (noMatchWeigher) {
            if (constraintMatchEnabled) {
                scoreImpacter = (a, b) -> {
                    JustificationsSupplier justificationsSupplier =
                            of(constraint, constraint.getJustificationMapping(), constraint.getIndictedObjectsMapping(), a, b);
                    return weightedScoreImpacter.impactScore(1, justificationsSupplier);
                };
            } else {
                scoreImpacter = (a, b) -> weightedScoreImpacter.impactScore(1, null);
            }
        } else {
            throw new IllegalStateException("Impossible state: neither of the supported match weighers provided.");
        }
        BiScorer<A, B> scorer = new BiScorer<>(constraint.getConstraintPackage(), constraint.getConstraintName(),
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
