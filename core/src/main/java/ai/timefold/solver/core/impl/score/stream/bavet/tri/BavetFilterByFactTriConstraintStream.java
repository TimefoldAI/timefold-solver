package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import java.util.Objects;

import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.bavet.tri.FilterTriNode;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ConstraintNodeBuildHelper;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class BavetFilterByFactTriConstraintStream<Solution_, A, B, C>
        extends BavetAbstractTriConstraintStream<Solution_, A, B, C> {

    private final TriPredicate<A, B, C> predicate;

    public BavetFilterByFactTriConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractTriConstraintStream<Solution_, A, B, C> parent, TriPredicate<A, B, C> predicate) {
        super(constraintFactory, parent);
        this.predicate = Objects.requireNonNull(predicate);
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(ConstraintNodeBuildHelper<Solution_, Score_> buildHelper) {
        var inputStoreIndex = buildHelper.reserveTupleStoreIndex(parent.getTupleSource());
        var node = new FilterTriNode<>(inputStoreIndex, predicate,
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
        } else if (o instanceof BavetFilterByFactTriConstraintStream<?, ?, ?, ?> other) {
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
