package ai.timefold.solver.constraint.streams.bavet.quad;

import java.util.Objects;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractFilterNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.api.function.QuadPredicate;

final class FilterQuadNode<A, B, C, D> extends AbstractFilterNode<QuadTuple<A, B, C, D>> {

    private final QuadPredicate<A, B, C, D> predicate;
    private final int outputStoreSize;

    public FilterQuadNode(int inputStoreIndex, QuadPredicate<A, B, C, D> predicate,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(inputStoreIndex, nextNodesTupleLifecycle);
        this.predicate = Objects.requireNonNull(predicate);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected QuadTuple<A, B, C, D> clone(QuadTuple<A, B, C, D> inTuple) {
        return new QuadTuple<>(inTuple.factA, inTuple.factB, inTuple.factC, inTuple.factD, outputStoreSize);
    }

    @Override
    protected void remap(QuadTuple<A, B, C, D> inTuple, QuadTuple<A, B, C, D> outTuple) {
        outTuple.updateIfDifferent(inTuple.factA, inTuple.factB, inTuple.factC, inTuple.factD);
    }

    @Override
    protected boolean testFiltering(QuadTuple<A, B, C, D> tuple) {
        return predicate.test(tuple.factA, tuple.factB, tuple.factC, tuple.factD);
    }

}
