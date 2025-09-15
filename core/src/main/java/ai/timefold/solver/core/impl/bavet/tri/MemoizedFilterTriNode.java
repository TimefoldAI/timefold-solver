package ai.timefold.solver.core.impl.bavet.tri;

import java.util.Objects;

import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.impl.bavet.common.AbstractMemoizedFilterNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class MemoizedFilterTriNode<A, B, C>
        extends AbstractMemoizedFilterNode<TriTuple<A, B, C>> {

    private final TriPredicate<A, B, C> predicate;

    public MemoizedFilterTriNode(int inputStoreIndex, TriPredicate<A, B, C> predicate,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle) {
        super(inputStoreIndex, nextNodesTupleLifecycle);
        this.predicate = Objects.requireNonNull(predicate);
    }

    @Override
    protected boolean testFiltering(TriTuple<A, B, C> tuple) {
        return predicate.test(tuple.factA, tuple.factB, tuple.factC);
    }

}
