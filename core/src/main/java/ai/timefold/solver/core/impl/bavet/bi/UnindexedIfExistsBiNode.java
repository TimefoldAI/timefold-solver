package ai.timefold.solver.core.impl.bavet.bi;

import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.impl.bavet.common.AbstractUnindexedIfExistsNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.InTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class UnindexedIfExistsBiNode<A, B, C> extends AbstractUnindexedIfExistsNode<BiTuple<A, B>, C> {

    private final TriPredicate<A, B, C> filtering;

    public UnindexedIfExistsBiNode(boolean shouldExist, TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle,
            InTupleStorePositionTracker tupleStorePositionTracker) {
        this(shouldExist, nextNodesTupleLifecycle, null, tupleStorePositionTracker);
    }

    public UnindexedIfExistsBiNode(boolean shouldExist, TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle,
            TriPredicate<A, B, C> filtering, InTupleStorePositionTracker tupleStorePositionTracker) {
        super(shouldExist, nextNodesTupleLifecycle, filtering != null, tupleStorePositionTracker);
        this.filtering = filtering;
    }

    @Override
    protected boolean testFiltering(BiTuple<A, B> leftTuple, UniTuple<C> rightTuple) {
        return filtering.test(leftTuple.getA(), leftTuple.getB(), rightTuple.getA());
    }

}
