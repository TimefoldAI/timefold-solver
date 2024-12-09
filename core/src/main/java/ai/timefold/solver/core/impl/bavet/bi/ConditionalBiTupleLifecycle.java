package ai.timefold.solver.core.impl.bavet.bi;

import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractConditionalTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

public final class ConditionalBiTupleLifecycle<A, B> extends AbstractConditionalTupleLifecycle<BiTuple<A, B>> {
    private final BiPredicate<A, B> predicate;

    public ConditionalBiTupleLifecycle(BiPredicate<A, B> predicate, TupleLifecycle<BiTuple<A, B>> tupleLifecycle) {
        super(tupleLifecycle);
        this.predicate = predicate;
    }

    @Override
    protected boolean test(BiTuple<A, B> tuple) {
        return predicate.test(tuple.factA, tuple.factB);
    }

}
