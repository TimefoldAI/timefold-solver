package ai.timefold.solver.constraint.streams.bavet.uni;

import java.util.Objects;
import java.util.function.Predicate;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractFilterNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;

final class FilterUniNode<A> extends AbstractFilterNode<UniTuple<A>> {

    private final Predicate<A> predicate;

    public FilterUniNode(int inputStoreIndex, Predicate<A> predicate, TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle) {
        super(inputStoreIndex, nextNodesTupleLifecycle);
        this.predicate = Objects.requireNonNull(predicate);
    }

    @Override
    protected boolean testFiltering(UniTuple<A> tuple) {
        return predicate.test(tuple.factA);
    }

}
