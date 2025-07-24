package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.bavet.uni.ForEachFromSolutionUniNode;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ForEachFromSolutionDataStream<Solution_, A>
        extends AbstractForEachDataStream<Solution_, A>
        implements TupleSource {

    private final ValueRangeDescriptor<Solution_> valueRangeDescriptor;

    public ForEachFromSolutionDataStream(DataStreamFactory<Solution_> dataStreamFactory, ValueRangeDescriptor<Solution_> valueRangeDescriptor, boolean includeNull) {
        super(dataStreamFactory, (Class<A>) valueRangeDescriptor.getVariableDescriptor().getVariablePropertyType(),
                includeNull);
        this.valueRangeDescriptor = Objects.requireNonNull(valueRangeDescriptor);
    }

    @Override
    protected AbstractForEachUniNode<A> getNode(TupleLifecycle<UniTuple<A>> tupleLifecycle, int outputStoreSize) {
        return new ForEachFromSolutionUniNode<>(valueRangeDescriptor, tupleLifecycle, outputStoreSize);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ForEachFromSolutionDataStream<?, ?> that &&
                Objects.equals(forEachClass, that.forEachClass) &&
                Objects.equals(valueRangeDescriptor, that.valueRangeDescriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(forEachClass, valueRangeDescriptor);
    }

    @Override
    public String toString() {
        return "ForEachFromSolution(" + valueRangeDescriptor + ") with " + childStreamList.size() + " children";
    }

}
