package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import static ai.timefold.solver.core.impl.bavet.common.TupleSourceRoot.LifecycleOperation;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.ForEachUnfilteredUniNode;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;

import org.jspecify.annotations.NullMarked;

@NullMarked
abstract sealed class AbstractForEachEnumeratingStream<Solution_, A>
        extends AbstractUniEnumeratingStream<Solution_, A>
        implements TupleSource
        permits ForEachIncludingPinnedEnumeratingStream {

    protected final Class<A> forEachClass;
    final boolean shouldIncludeNull;

    protected AbstractForEachEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            Class<A> forEachClass,
            boolean includeNull) {
        super(enumeratingStreamFactory, null);
        this.forEachClass = Objects.requireNonNull(forEachClass);
        this.shouldIncludeNull = includeNull;
    }

    @Override
    public final void collectActiveEnumeratingStreams(Set<AbstractEnumeratingStream<Solution_>> enumeratingStreamSet) {
        enumeratingStreamSet.add(this);
    }

    @Override
    public final void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        TupleLifecycle<UniTuple<A>> tupleLifecycle = buildHelper.getAggregatedTupleLifecycle(childStreamList);
        var outputStoreSize = buildHelper.extractTupleStoreSize(this);
        var node = new ForEachUnfilteredUniNode<>(forEachClass, tupleLifecycle, outputStoreSize);
        if (shouldIncludeNull && node.supports(LifecycleOperation.INSERT)) {
            node.insert(null);
        }
        buildHelper.addNode(node, this, null);
    }

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

}
