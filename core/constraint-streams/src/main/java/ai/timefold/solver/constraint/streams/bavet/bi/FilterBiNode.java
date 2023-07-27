package ai.timefold.solver.constraint.streams.bavet.bi;

import java.util.Objects;
import java.util.function.BiPredicate;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractFilterNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.BiTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;

final class FilterBiNode<A, B> extends AbstractFilterNode<BiTuple<A, B>> {

    private final BiPredicate<A, B> predicate;
    private final int outputStoreSize;

    public FilterBiNode(int inputStoreIndex, BiPredicate<A, B> predicate, TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle,
            int outputStoreSize) {
        super(inputStoreIndex, nextNodesTupleLifecycle);
        this.predicate = Objects.requireNonNull(predicate);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected BiTuple<A, B> clone(BiTuple<A, B> inTuple) {
        return new BiTuple<>(inTuple.factA, inTuple.factB, outputStoreSize);
    }

    @Override
    protected void remap(BiTuple<A, B> inTuple, BiTuple<A, B> outTuple) {
        outTuple.updateIfDifferent(inTuple.factA, inTuple.factB);
    }

    @Override
    protected boolean testFiltering(BiTuple<A, B> tuple) {
        return predicate.test(tuple.factA, tuple.factB);
    }

}
