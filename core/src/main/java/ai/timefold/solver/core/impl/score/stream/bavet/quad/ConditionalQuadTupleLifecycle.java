package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractConditionalTupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;

final class ConditionalQuadTupleLifecycle<A, B, C, D> extends AbstractConditionalTupleLifecycle<QuadTuple<A, B, C, D>> {
    private final QuadPredicate<A, B, C, D> predicate;

    public ConditionalQuadTupleLifecycle(QuadPredicate<A, B, C, D> predicate,
            TupleLifecycle<QuadTuple<A, B, C, D>> tupleLifecycle) {
        super(tupleLifecycle);
        this.predicate = predicate;
    }

    @Override
    protected boolean test(QuadTuple<A, B, C, D> tuple) {
        return predicate.test(tuple.factA, tuple.factB, tuple.factC, tuple.factD);
    }
}
