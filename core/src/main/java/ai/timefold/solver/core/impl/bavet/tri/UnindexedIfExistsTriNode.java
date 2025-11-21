package ai.timefold.solver.core.impl.bavet.tri;

import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.impl.bavet.common.AbstractUnindexedIfExistsNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.InTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class UnindexedIfExistsTriNode<A, B, C, D> extends AbstractUnindexedIfExistsNode<TriTuple<A, B, C>, D> {

    private final QuadPredicate<A, B, C, D> filtering;

    public UnindexedIfExistsTriNode(boolean shouldExist, TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle,
            InTupleStorePositionTracker tupleStorePositionTracker) {
        this(shouldExist, nextNodesTupleLifecycle, null, tupleStorePositionTracker);
    }

    public UnindexedIfExistsTriNode(boolean shouldExist, TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle,
            QuadPredicate<A, B, C, D> filtering, InTupleStorePositionTracker tupleStorePositionTracker) {
        super(shouldExist, nextNodesTupleLifecycle, filtering != null, tupleStorePositionTracker);
        this.filtering = filtering;
    }

    @Override
    protected boolean testFiltering(TriTuple<A, B, C> leftTuple, UniTuple<D> rightTuple) {
        return filtering.test(leftTuple.factA, leftTuple.factB, leftTuple.factC, rightTuple.factA);
    }

}
