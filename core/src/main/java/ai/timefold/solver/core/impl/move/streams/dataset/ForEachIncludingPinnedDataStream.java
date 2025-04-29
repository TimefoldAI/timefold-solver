package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.bavet.uni.ForEachIncludingUnassignedUniNode;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ForEachIncludingPinnedDataStream<Solution_, A>
        extends AbstractForEachDataStream<Solution_, A>
        implements TupleSource {

    public ForEachIncludingPinnedDataStream(DataStreamFactory<Solution_> dataStreamFactory, Class<A> forEachClass) {
        super(dataStreamFactory, forEachClass);
    }

    @Override
    protected AbstractForEachUniNode<A> getNode(TupleLifecycle<UniTuple<A>> tupleLifecycle, int outputStoreSize) {
        return new ForEachIncludingUnassignedUniNode<>(forEachClass, tupleLifecycle, outputStoreSize);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ForEachIncludingPinnedDataStream<?, ?> that &&
                Objects.equals(forEachClass, that.forEachClass);
    }

    @Override
    public int hashCode() {
        return forEachClass.hashCode();
    }

    @Override
    public String toString() {
        return "ForEach (" + forEachClass.getSimpleName() + ") with " + childStreamList.size() + " children";
    }

}
