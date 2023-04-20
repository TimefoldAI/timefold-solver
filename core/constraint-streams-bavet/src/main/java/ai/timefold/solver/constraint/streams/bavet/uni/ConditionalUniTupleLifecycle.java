package ai.timefold.solver.constraint.streams.bavet.uni;

import java.util.function.Predicate;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractConditionalTupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.TupleLifecycle;

final class ConditionalUniTupleLifecycle<A> extends AbstractConditionalTupleLifecycle<UniTuple<A>> {
    private final Predicate<A> predicate;

    public ConditionalUniTupleLifecycle(Predicate<A> predicate, TupleLifecycle<UniTuple<A>> tupleLifecycle) {
        super(tupleLifecycle);
        this.predicate = predicate;
    }

    @Override
    protected boolean test(UniTuple<A> tuple) {
        return predicate.test(tuple.getFactA());
    }
}
