package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.util.Objects;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

public final class ForEachExcludingUnassignedUniNode<A> extends AbstractForEachUniNode<A> {

    private final Predicate<A> filter;

    public ForEachExcludingUnassignedUniNode(Class<A> forEachClass, Predicate<A> filter,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(forEachClass, nextNodesTupleLifecycle, outputStoreSize);
        this.filter = Objects.requireNonNullElse(filter, a -> true);
    }

    @Override
    public void insert(A a) {
        if (!filter.test(a)) { // Skip inserting the tuple as it does not pass the filter.
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
            super.retract(a); // Call super.retract() to avoid testing the filter again.
        }
    }

    @Override
    public void retract(A a) {
        if (!filter.test(a)) { // The tuple was never inserted because it did not pass the filter.
            return;
        }
        super.retract(a);
    }

}
