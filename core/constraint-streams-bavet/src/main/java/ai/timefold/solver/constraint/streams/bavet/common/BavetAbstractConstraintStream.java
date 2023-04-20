package ai.timefold.solver.constraint.streams.bavet.common;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraint;
import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.uni.BavetAbstractUniConstraintStream;
import ai.timefold.solver.constraint.streams.common.AbstractConstraintStream;
import ai.timefold.solver.constraint.streams.common.RetrievalSemantics;
import ai.timefold.solver.constraint.streams.common.ScoreImpactType;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;

public abstract class BavetAbstractConstraintStream<Solution_> extends AbstractConstraintStream<Solution_> {

    protected final BavetConstraintFactory<Solution_> constraintFactory;

    public BavetAbstractConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            RetrievalSemantics retrievalSemantics) {
        super(retrievalSemantics);
        this.constraintFactory = constraintFactory;
    }

    // ************************************************************************
    // Penalize/reward
    // ************************************************************************

    protected Constraint buildConstraint(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ScoreImpactType impactType, Object justificationFunction, Object indictedObjectsMapping,
            BavetScoringConstraintStream<Solution_> stream) {
        var resolvedConstraintPackage =
                Objects.requireNonNullElseGet(constraintPackage, this.constraintFactory::getDefaultConstraintPackage);
        var resolvedJustificationMapping =
                Objects.requireNonNullElseGet(justificationFunction, this::getDefaultJustificationMapping);
        var resolvedIndictedObjectsMapping =
                Objects.requireNonNullElseGet(indictedObjectsMapping, this::getDefaultIndictedObjectsMapping);
        var isConstraintWeightConfigurable = constraintWeight == null;
        var constraintWeightExtractor = isConstraintWeightConfigurable
                ? buildConstraintWeightExtractor(resolvedConstraintPackage, constraintName)
                : buildConstraintWeightExtractor(resolvedConstraintPackage, constraintName, constraintWeight);
        var constraint =
                new BavetConstraint<>(constraintFactory, resolvedConstraintPackage, constraintName, constraintWeightExtractor,
                        impactType, resolvedJustificationMapping, resolvedIndictedObjectsMapping,
                        isConstraintWeightConfigurable, stream);
        stream.setConstraint(constraint);
        return constraint;
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    public abstract void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet);

    public BavetAbstractConstraintStream<Solution_> getTupleSource() {
        return this;
    }

    public abstract <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper);

    // ************************************************************************
    // Helper methods
    // ************************************************************************

    protected <A> BavetAbstractUniConstraintStream<Solution_, A> assertBavetUniConstraintStream(
            UniConstraintStream<A> otherStream) {
        if (!(otherStream instanceof BavetAbstractUniConstraintStream)) {
            throw new IllegalStateException("The streams (" + this + ", " + otherStream
                    + ") are not built from the same " + ConstraintFactory.class.getSimpleName() + ".");
        }
        BavetAbstractUniConstraintStream<Solution_, A> other = (BavetAbstractUniConstraintStream<Solution_, A>) otherStream;
        if (constraintFactory != other.getConstraintFactory()) {
            throw new IllegalStateException("The streams (" + this + ", " + other
                    + ") are built from different constraintFactories (" + constraintFactory + ", "
                    + other.getConstraintFactory()
                    + ").");
        }
        return other;
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public BavetConstraintFactory<Solution_> getConstraintFactory() {
        return constraintFactory;
    }

}
