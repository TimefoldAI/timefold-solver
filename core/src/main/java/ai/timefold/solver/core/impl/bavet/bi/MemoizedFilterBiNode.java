package ai.timefold.solver.core.impl.bavet.bi;

import java.util.Objects;
import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.bavet.common.AbstractMemoizedFilterNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class MemoizedFilterBiNode<A, B>
        extends AbstractMemoizedFilterNode<BiTuple<A, B>> {

    private final BiPredicate<A, B> predicate;

    public MemoizedFilterBiNode(int inputStoreIndex, BiPredicate<A, B> predicate,
            TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle) {
        super(inputStoreIndex, nextNodesTupleLifecycle);
        this.predicate = Objects.requireNonNull(predicate);
    }

    @Override
    protected boolean testFiltering(BiTuple<A, B> tuple) {
        return predicate.test(tuple.factA, tuple.factB);
    }

}
