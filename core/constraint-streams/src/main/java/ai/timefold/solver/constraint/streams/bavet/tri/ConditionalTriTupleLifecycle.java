package ai.timefold.solver.constraint.streams.bavet.tri;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractConditionalTupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TriTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.api.function.TriPredicate;

final class ConditionalTriTupleLifecycle<A, B, C> extends AbstractConditionalTupleLifecycle<TriTuple<A, B, C>> {
    private final TriPredicate<A, B, C> predicate;

    public ConditionalTriTupleLifecycle(TriPredicate<A, B, C> predicate, TupleLifecycle<TriTuple<A, B, C>> tupleLifecycle) {
        super(tupleLifecycle);
        this.predicate = predicate;
    }

    @Override
    protected boolean test(TriTuple<A, B, C> tuple) {
        return predicate.test(tuple.factA, tuple.factB, tuple.factC);
    }

}
