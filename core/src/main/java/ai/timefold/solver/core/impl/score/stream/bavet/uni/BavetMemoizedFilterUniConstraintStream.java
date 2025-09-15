package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.util.Objects;
import java.util.function.Predicate;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.bavet.uni.MemoizedFilterUniNode;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ConstraintNodeBuildHelper;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class BavetMemoizedFilterUniConstraintStream<Solution_, A>
        extends BavetAbstractUniConstraintStream<Solution_, A> {

    private final Predicate<A> predicate;

    public BavetMemoizedFilterUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractUniConstraintStream<Solution_, A> parent, Predicate<A> predicate) {
        super(constraintFactory, parent);
        this.predicate = Objects.requireNonNull(predicate);
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(ConstraintNodeBuildHelper<Solution_, Score_> buildHelper) {
        var inputStoreIndex = buildHelper.reserveTupleStoreIndex(parent.getTupleSource());
        var node = new MemoizedFilterUniNode<>(inputStoreIndex, predicate,
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
        } else if (o instanceof BavetMemoizedFilterUniConstraintStream<?, ?> other) {
            return parent == other.parent
                    && predicate == other.predicate;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "MemoizedFilter() with " + childStreamList.size() + " children";
    }

}
