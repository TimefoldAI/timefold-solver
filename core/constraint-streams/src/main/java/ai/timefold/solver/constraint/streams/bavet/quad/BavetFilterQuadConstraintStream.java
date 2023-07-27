package ai.timefold.solver.constraint.streams.bavet.quad;

import java.util.Objects;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.common.NodeBuildHelper;
import ai.timefold.solver.constraint.streams.bavet.common.TupleSource;
import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.score.Score;

final class BavetFilterQuadConstraintStream<Solution_, A, B, C, D>
        extends BavetAbstractQuadConstraintStream<Solution_, A, B, C, D>
        implements TupleSource {

    private final QuadPredicate<A, B, C, D> predicate;

    public BavetFilterQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> parent,
            QuadPredicate<A, B, C, D> predicate) {
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
        int outputStoreSize = buildHelper.extractTupleStoreSize(this);
        var node = new FilterQuadNode<>(inputStoreIndex, predicate, buildHelper.getAggregatedTupleLifecycle(childStreamList),
                outputStoreSize);
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
        } else if (o instanceof BavetFilterQuadConstraintStream) {
            BavetFilterQuadConstraintStream<?, ?, ?, ?, ?> other = (BavetFilterQuadConstraintStream<?, ?, ?, ?, ?>) o;
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
