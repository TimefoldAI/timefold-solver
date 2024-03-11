package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractConditionalTupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;

final class ConditionalBiTupleLifecycle<A, B> extends AbstractConditionalTupleLifecycle<BiTuple<A, B>> {
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
