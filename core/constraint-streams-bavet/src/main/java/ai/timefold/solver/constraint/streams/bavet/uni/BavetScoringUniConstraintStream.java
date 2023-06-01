package ai.timefold.solver.constraint.streams.bavet.uni;

import static ai.timefold.solver.constraint.streams.common.inliner.JustificationsSupplier.of;

import java.math.BigDecimal;
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

    private final boolean noMatchWeigher;
    private final ToIntFunction<A> intMatchWeigher;
    private final ToLongFunction<A> longMatchWeigher;
    private final Function<A, BigDecimal> bigDecimalMatchWeigher;
    private BavetConstraint<Solution_> constraint;

    public BavetScoringUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractUniConstraintStream<Solution_, A> parent,
            ToIntFunction<A> intMatchWeigher) {
        this(constraintFactory, parent, false, intMatchWeigher, null, null);
        if (intMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    public BavetScoringUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractUniConstraintStream<Solution_, A> parent,
            ToLongFunction<A> longMatchWeigher) {
        this(constraintFactory, parent, false, null, longMatchWeigher, null);
        if (longMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    public BavetScoringUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractUniConstraintStream<Solution_, A> parent,
            Function<A, BigDecimal> bigDecimalMatchWeigher) {
        this(constraintFactory, parent, false, null, null, bigDecimalMatchWeigher);
        if (bigDecimalMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    private BavetScoringUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractUniConstraintStream<Solution_, A> parent,
            boolean noMatchWeigher, ToIntFunction<A> intMatchWeigher, ToLongFunction<A> longMatchWeigher,
            Function<A, BigDecimal> bigDecimalMatchWeigher) {
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
        Function<A, UndoScoreImpacter> scoreImpacter;
        if (intMatchWeigher != null) {
            if (constraintMatchEnabled) {
                scoreImpacter = a -> {
                    int matchWeight = intMatchWeigher.applyAsInt(a);
                    constraint.assertCorrectImpact(matchWeight);
                    JustificationsSupplier justificationsSupplier =
                            of(constraint, constraint.getJustificationMapping(), constraint.getIndictedObjectsMapping(), a);
                    return weightedScoreImpacter.impactScore(matchWeight, justificationsSupplier);
                };
            } else {
                scoreImpacter = a -> {
                    int matchWeight = intMatchWeigher.applyAsInt(a);
                    constraint.assertCorrectImpact(matchWeight);
                    return weightedScoreImpacter.impactScore(matchWeight, null);
                };
            }
        } else if (longMatchWeigher != null) {
            if (constraintMatchEnabled) {
                scoreImpacter = a -> {
                    long matchWeight = longMatchWeigher.applyAsLong(a);
                    constraint.assertCorrectImpact(matchWeight);
                    JustificationsSupplier justificationsSupplier =
                            of(constraint, constraint.getJustificationMapping(), constraint.getIndictedObjectsMapping(), a);
                    return weightedScoreImpacter.impactScore(matchWeight, justificationsSupplier);
                };
            } else {
                scoreImpacter = a -> {
                    long matchWeight = longMatchWeigher.applyAsLong(a);
                    constraint.assertCorrectImpact(matchWeight);
                    return weightedScoreImpacter.impactScore(matchWeight, null);
                };
            }
        } else if (bigDecimalMatchWeigher != null) {
            if (constraintMatchEnabled) {
                scoreImpacter = a -> {
                    BigDecimal matchWeight = bigDecimalMatchWeigher.apply(a);
                    constraint.assertCorrectImpact(matchWeight);
                    JustificationsSupplier justificationsSupplier =
                            of(constraint, constraint.getJustificationMapping(), constraint.getIndictedObjectsMapping(), a);
                    return weightedScoreImpacter.impactScore(matchWeight, justificationsSupplier);
                };
            } else {
                scoreImpacter = a -> {
                    BigDecimal matchWeight = bigDecimalMatchWeigher.apply(a);
                    constraint.assertCorrectImpact(matchWeight);
                    return weightedScoreImpacter.impactScore(matchWeight, null);
                };
            }
        } else if (noMatchWeigher) {
            if (constraintMatchEnabled) {
                scoreImpacter = a -> {
                    JustificationsSupplier justificationsSupplier =
                            of(constraint, constraint.getJustificationMapping(), constraint.getIndictedObjectsMapping(), a);
                    return weightedScoreImpacter.impactScore(1, justificationsSupplier);
                };
            } else {
                scoreImpacter = a -> weightedScoreImpacter.impactScore(1, null);
            }
        } else {
            throw new IllegalStateException("Impossible state: neither of the supported match weighers provided.");
        }
        UniScorer<A> scorer = new UniScorer<>(constraint.getConstraintPackage(), constraint.getConstraintName(),
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
