package ai.timefold.solver.constraint.streams.bavet.tri;

import java.util.Objects;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.api.score.Score;

final class BavetFilterTriConstraintStream<Solution_, A, B, C>
        extends BavetAbstractTriConstraintStream<Solution_, A, B, C> {

    private final TriPredicate<A, B, C> predicate;

    public BavetFilterTriConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractTriConstraintStream<Solution_, A, B, C> parent,
            TriPredicate<A, B, C> predicate) {
        super(constraintFactory, parent);
        this.predicate = predicate;
        if (predicate == null) {
            throw new IllegalArgumentException("The predicate (null) cannot be null.");
        }
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper) {
        int inputStoreIndex = buildHelper.reserveTupleStoreIndex(parent.getTupleSource());
        var node = new FilterTriNode<>(inputStoreIndex, predicate, buildHelper.getAggregatedTupleLifecycle(childStreamList));
        buildHelper.addNode(node, this);
    }

    // ************************************************************************
    // Equality for node sharing
    // ************************************************************************

    @Override
    public int hashCode() {
        return Objects.hash(parent, predicate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof BavetFilterTriConstraintStream) {
            BavetFilterTriConstraintStream<?, ?, ?, ?> other = (BavetFilterTriConstraintStream<?, ?, ?, ?>) o;
            return parent == other.parent
                    && predicate == other.predicate;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Filter() with " + childStreamList.size() + " children";
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

}
