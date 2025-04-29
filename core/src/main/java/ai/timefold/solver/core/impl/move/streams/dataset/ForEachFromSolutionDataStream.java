package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.bavet.uni.ForEachFromSolutionUniNode;
import ai.timefold.solver.core.impl.move.streams.FromSolutionValueCollectingFunction;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ForEachFromSolutionDataStream<Solution_, A>
        extends AbstractForEachDataStream<Solution_, A>
        implements TupleSource {

    private final FromSolutionValueCollectingFunction<Solution_, A> valueCollectingFunction;

    public ForEachFromSolutionDataStream(DataStreamFactory<Solution_> dataStreamFactory,
            FromSolutionValueCollectingFunction<Solution_, A> valueCollectingFunction) {
        super(dataStreamFactory, Objects.requireNonNull(valueCollectingFunction).declaredClass());
        this.valueCollectingFunction = valueCollectingFunction;
    }

    @Override
    protected AbstractForEachUniNode<A> getNode(TupleLifecycle<UniTuple<A>> tupleLifecycle, int outputStoreSize) {
        return new ForEachFromSolutionUniNode<>(valueCollectingFunction, tupleLifecycle, outputStoreSize);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ForEachFromSolutionDataStream<?, ?> that &&
                Objects.equals(forEachClass, that.forEachClass) &&
                Objects.equals(valueCollectingFunction, that.valueCollectingFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(forEachClass, valueCollectingFunction);
    }

    @Override
    public String toString() {
        return "ForEachFromSolution(" + valueCollectingFunction + ") with " + childStreamList.size() + " children";
    }

}
