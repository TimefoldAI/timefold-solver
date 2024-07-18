package ai.timefold.solver.core.impl.score.stream.bavet.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraint;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;

public abstract class BavetAbstractConstraintStream<Solution_> extends AbstractConstraintStream<Solution_> {

    protected final BavetConstraintFactory<Solution_> constraintFactory;
    protected final BavetAbstractConstraintStream<Solution_> parent;
    protected final List<BavetAbstractConstraintStream<Solution_>> childStreamList = new ArrayList<>(2);

    protected BavetAbstractConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractConstraintStream<Solution_> parent) {
        super(parent.getRetrievalSemantics());
        this.constraintFactory = constraintFactory;
        this.parent = parent;
    }

    protected BavetAbstractConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            RetrievalSemantics retrievalSemantics) {
        super(retrievalSemantics);
        this.constraintFactory = constraintFactory;
        this.parent = null;
    }

    /**
     * Whether the stream guarantees that no two tuples it produces will ever have the same set of facts.
     * Streams which can prove that they either do or do not produce unique tuples should override this method.
     *
     * @return delegates to {@link #getParent()} if not null, otherwise false
     */
    public boolean guaranteesDistinct() {
        if (parent != null) {
            // It is generally safe to take this from the parent; if the stream disagrees, it may override.
            return parent.guaranteesDistinct();
        } else { // Streams need to explicitly opt-in by overriding this method.
            return false;
        }
    }

    // ************************************************************************
    // Penalize/reward
    // ************************************************************************

    @SuppressWarnings("unchecked")
    protected <Score_ extends Score<Score_>> Constraint buildConstraint(String constraintPackage, String constraintName,
            String description, Score_ constraintWeight, ScoreImpactType impactType, Object justificationFunction,
            Object indictedObjectsMapping,
            BavetScoringConstraintStream<Solution_> stream) {
        var resolvedConstraintPackage =
                Objects.requireNonNullElseGet(constraintPackage, this.constraintFactory::getDefaultConstraintPackage);
        var resolvedJustificationMapping =
                Objects.requireNonNullElseGet(justificationFunction, this::getDefaultJustificationMapping);
        var resolvedIndictedObjectsMapping =
                Objects.requireNonNullElseGet(indictedObjectsMapping, this::getDefaultIndictedObjectsMapping);
        var isConstraintWeightConfigurable = constraintWeight == null;
        var constraintRef = ConstraintRef.of(resolvedConstraintPackage, constraintName);
        var constraint = new BavetConstraint<>(constraintFactory, constraintRef, description,
                isConstraintWeightConfigurable ? null : constraintWeight, impactType, resolvedJustificationMapping,
                resolvedIndictedObjectsMapping, stream);
        stream.setConstraint(constraint);
        return constraint;
    }

    // ************************************************************************
    // Stream builder methods
    // ************************************************************************

    public final <Stream_ extends BavetAbstractConstraintStream<Solution_>> Stream_ shareAndAddChild(Stream_ stream) {
        return constraintFactory.share(stream, childStreamList::add);
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    public void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet) {
        if (parent == null) { // Maybe a join/ifExists/forEach forgot to override this?
            throw new IllegalStateException("Impossible state: the stream (" + this + ") does not have a parent.");
        }
        parent.collectActiveConstraintStreams(constraintStreamSet);
        constraintStreamSet.add(this);
    }

    /**
     * Returns the stream which first produced the tuple that this stream operates on.
     * If a stream does not have a single parent nor is it a source, it is expected to override this method.
     *
     * @return this if {@link TupleSource}, otherwise parent's tuple source.
     */
    public BavetAbstractConstraintStream<Solution_> getTupleSource() {
        if (this instanceof TupleSource) {
            return this;
        } else if (parent == null) { // Maybe some stream forgot to override this?
            throw new IllegalStateException("Impossible state: the stream (" + this + ") does not have a parent.");
        }
        return parent.getTupleSource();
    }

    public abstract <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper);

    // ************************************************************************
    // Helper methods
    // ************************************************************************

    protected void assertEmptyChildStreamList() {
        if (!childStreamList.isEmpty()) {
            throw new IllegalStateException(
                    "Impossible state: the stream (" + this + ") has a non-empty childStreamList (" + childStreamList + ").");
        }
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public BavetConstraintFactory<Solution_> getConstraintFactory() {
        return constraintFactory;
    }

    /**
     * @return null for join/ifExists nodes, which have left and right parents instead;
     *         also null for forEach node, which has no parent.
     */
    public final BavetAbstractConstraintStream<Solution_> getParent() {
        return parent;
    }

    public final List<BavetAbstractConstraintStream<Solution_>> getChildStreamList() {
        return childStreamList;
    }

}
