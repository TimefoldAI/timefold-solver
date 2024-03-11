package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.util.function.Predicate;

import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractConditionalTupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class ConditionalUniTupleLifecycle<A> extends AbstractConditionalTupleLifecycle<UniTuple<A>> {
    private final Predicate<A> predicate;

    public ConditionalUniTupleLifecycle(Predicate<A> predicate, TupleLifecycle<UniTuple<A>> tupleLifecycle) {
        super(tupleLifecycle);
        this.predicate = predicate;
    }

    @Override
    protected boolean test(UniTuple<A> tuple) {
        return predicate.test(tuple.factA);
    }
}
