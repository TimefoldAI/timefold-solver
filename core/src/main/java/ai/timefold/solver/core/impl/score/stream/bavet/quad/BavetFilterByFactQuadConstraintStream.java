package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import java.util.Objects;

import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.bavet.quad.FilterQuadNode;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ConstraintNodeBuildHelper;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class BavetFilterByFactQuadConstraintStream<Solution_, A, B, C, D>
        extends BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> {

    private final QuadPredicate<A, B, C, D> predicate;

    public BavetFilterByFactQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> parent, QuadPredicate<A, B, C, D> predicate) {
        super(constraintFactory, parent);
        this.predicate = Objects.requireNonNull(predicate);
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(ConstraintNodeBuildHelper<Solution_, Score_> buildHelper) {
        var inputStoreIndex = buildHelper.reserveTupleStoreIndex(parent.getTupleSource());
        var node =
                new FilterQuadNode<>(inputStoreIndex, predicate,
                        buildHelper.getAggregatedTupleLifecycle(childStreamList));
        buildHelper.addNode(node, this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, predicate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof BavetFilterByFactQuadConstraintStream<?, ?, ?, ?, ?> other) {
            return parent == other.parent
                    && predicate == other.predicate;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "FilterByFact() with " + childStreamList.size() + " children";
    }

}
