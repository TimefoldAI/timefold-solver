package ai.timefold.solver.constraint.streams.bavet.uni;

import java.util.Objects;
import java.util.function.Predicate;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;

public final class ForEachExcludingNullVarsUniNode<A> extends AbstractForEachUniNode<A> {

    private final Predicate<A> filter;

    public ForEachExcludingNullVarsUniNode(Class<A> forEachClass, Predicate<A> filter,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(forEachClass, nextNodesTupleLifecycle, outputStoreSize);
        this.filter = Objects.requireNonNull(filter);
    }

    @Override
    public void insert(A a) {
        if (!filter.test(a)) {
            return;
        }
        super.insert(a);
    }

    @Override
    public void update(A a) {
        UniTuple<A> tuple = tupleMap.get(a);
        if (tuple == null) { // The tuple was never inserted because it did not pass the filter.
            insert(a);
        } else if (filter.test(a)) {
            innerUpdate(a, tuple);
        } else {
            retract(a);
        }
    }

    @Override
    public void retract(A a) {
        UniTuple<A> tuple = tupleMap.remove(a);
        if (tuple == null) { // The tuple was never inserted because it did not pass the filter.
            return;
        }
        innerRetract(a, tuple);
    }

}
