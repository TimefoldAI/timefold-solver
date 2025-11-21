package ai.timefold.solver.core.impl.bavet.quad;

import ai.timefold.solver.core.api.function.PentaPredicate;
import ai.timefold.solver.core.impl.bavet.common.AbstractUnindexedIfExistsNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.InTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class UnindexedIfExistsQuadNode<A, B, C, D, E> extends AbstractUnindexedIfExistsNode<QuadTuple<A, B, C, D>, E> {

    private final PentaPredicate<A, B, C, D, E> filtering;

    public UnindexedIfExistsQuadNode(boolean shouldExist, TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle,
            InTupleStorePositionTracker tupleStorePositionTracker) {
        this(shouldExist, nextNodesTupleLifecycle, null, tupleStorePositionTracker);
    }

    public UnindexedIfExistsQuadNode(boolean shouldExist, TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle,
            PentaPredicate<A, B, C, D, E> filtering, InTupleStorePositionTracker tupleStorePositionTracker) {
        super(shouldExist, nextNodesTupleLifecycle, filtering != null, tupleStorePositionTracker);
        this.filtering = filtering;
    }

    @Override
    protected boolean testFiltering(QuadTuple<A, B, C, D> leftTuple, UniTuple<E> rightTuple) {
        return filtering.test(leftTuple.factA, leftTuple.factB, leftTuple.factC, leftTuple.factD,
                rightTuple.factA);
    }

}
