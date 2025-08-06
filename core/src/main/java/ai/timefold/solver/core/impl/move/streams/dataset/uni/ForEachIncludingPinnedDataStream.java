package ai.timefold.solver.core.impl.move.streams.dataset.uni;

import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.bavet.uni.ForEachIncludingUnassignedUniNode;
import ai.timefold.solver.core.impl.move.streams.dataset.DataStreamFactory;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public final class ForEachIncludingPinnedDataStream<Solution_, A>
        extends AbstractForEachDataStream<Solution_, A>
        implements TupleSource {

    public ForEachIncludingPinnedDataStream(DataStreamFactory<Solution_> dataStreamFactory, Class<A> forEachClass, boolean includeNull) {
        super(dataStreamFactory, forEachClass, includeNull);
    }

    @Override
    protected AbstractForEachUniNode<A> getNode(TupleLifecycle<UniTuple<A>> tupleLifecycle, int outputStoreSize) {
        return new ForEachIncludingUnassignedUniNode<>(forEachClass, tupleLifecycle, outputStoreSize);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ForEachIncludingPinnedDataStream<?, ?> that &&
                Objects.equals(shouldIncludeNull, that.shouldIncludeNull) &&
                Objects.equals(forEachClass, that.forEachClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shouldIncludeNull, forEachClass);
    }

    @Override
    public String toString() {
        return "ForEach (" + forEachClass.getSimpleName() + ") with " + childStreamList.size() + " children";
    }

}
