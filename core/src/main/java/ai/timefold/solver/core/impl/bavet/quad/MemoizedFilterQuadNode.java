package ai.timefold.solver.core.impl.bavet.quad;

import java.util.Objects;

import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.impl.bavet.common.AbstractMemoizedFilterNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class MemoizedFilterQuadNode<A, B, C, D>
        extends AbstractMemoizedFilterNode<QuadTuple<A, B, C, D>> {

    private final QuadPredicate<A, B, C, D> predicate;

    public MemoizedFilterQuadNode(int inputStoreIndex, QuadPredicate<A, B, C, D> predicate,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle) {
        super(inputStoreIndex, nextNodesTupleLifecycle);
        this.predicate = Objects.requireNonNull(predicate);
    }

    @Override
    protected boolean testFiltering(QuadTuple<A, B, C, D> tuple) {
        return predicate.test(tuple.factA, tuple.factB, tuple.factC, tuple.factD);
    }

}
