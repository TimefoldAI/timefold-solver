package ai.timefold.solver.constraint.streams.bavet.bi;

import java.util.Objects;
import java.util.function.BiPredicate;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractFilterNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.BiTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;

final class FilterBiNode<A, B> extends AbstractFilterNode<BiTuple<A, B>> {

    private final BiPredicate<A, B> predicate;

    public FilterBiNode(int inputStoreIndex, BiPredicate<A, B> predicate,
            TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle) {
        super(inputStoreIndex, nextNodesTupleLifecycle);
        this.predicate = Objects.requireNonNull(predicate);
    }

    @Override
    protected boolean testFiltering(BiTuple<A, B> tuple) {
        return predicate.test(tuple.factA, tuple.factB);
    }

}
