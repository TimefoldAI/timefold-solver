package ai.timefold.solver.constraint.streams.bavet.tri;

import java.util.Objects;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractFilterNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TriTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.api.function.TriPredicate;

final class FilterTriNode<A, B, C> extends AbstractFilterNode<TriTuple<A, B, C>> {

    private final TriPredicate<A, B, C> predicate;
    private final int outputStoreSize;

    public FilterTriNode(int inputStoreIndex, TriPredicate<A, B, C> predicate,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(inputStoreIndex, nextNodesTupleLifecycle);
        this.predicate = Objects.requireNonNull(predicate);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected TriTuple<A, B, C> clone(TriTuple<A, B, C> inTuple) {
        return new TriTuple<>(inTuple.factA, inTuple.factB, inTuple.factC, outputStoreSize);
    }

    @Override
    protected boolean testFiltering(TriTuple<A, B, C> tuple) {
        return predicate.test(tuple.factA, tuple.factB, tuple.factC);
    }

}
