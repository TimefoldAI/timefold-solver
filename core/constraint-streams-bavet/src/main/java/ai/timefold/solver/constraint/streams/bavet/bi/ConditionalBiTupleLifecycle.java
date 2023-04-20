package ai.timefold.solver.constraint.streams.bavet.bi;

import java.util.function.BiPredicate;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractConditionalTupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.TupleLifecycle;

final class ConditionalBiTupleLifecycle<A, B> extends AbstractConditionalTupleLifecycle<BiTuple<A, B>> {
    private final BiPredicate<A, B> predicate;

    public ConditionalBiTupleLifecycle(BiPredicate<A, B> predicate, TupleLifecycle<BiTuple<A, B>> tupleLifecycle) {
        super(tupleLifecycle);
        this.predicate = predicate;
    }

    @Override
    protected boolean test(BiTuple<A, B> tuple) {
        return predicate.test(tuple.getFactA(), tuple.getFactB());
    }

}
