package ai.timefold.solver.constraint.streams.bavet.uni;

import java.util.Objects;
import java.util.function.Predicate;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractFilterNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;

final class FilterUniNode<A> extends AbstractFilterNode<UniTuple<A>> {

    private final Predicate<A> predicate;
    private final int outputStoreSize;

    public FilterUniNode(int inputStoreIndex, Predicate<A> predicate, TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle,
            int outputStoreSize) {
        super(inputStoreIndex, nextNodesTupleLifecycle);
        this.predicate = Objects.requireNonNull(predicate);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected UniTuple<A> clone(UniTuple<A> inTuple) {
        return new UniTuple<>(inTuple.factA, outputStoreSize);
    }

    @Override
    protected boolean testFiltering(UniTuple<A> tuple) {
        return predicate.test(tuple.factA);
    }

}
