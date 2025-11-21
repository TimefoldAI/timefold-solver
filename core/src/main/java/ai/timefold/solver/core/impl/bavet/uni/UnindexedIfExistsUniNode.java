package ai.timefold.solver.core.impl.bavet.uni;

import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.bavet.common.AbstractUnindexedIfExistsNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.InTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class UnindexedIfExistsUniNode<A, B> extends AbstractUnindexedIfExistsNode<UniTuple<A>, B> {

    private final BiPredicate<A, B> filtering;

    public UnindexedIfExistsUniNode(boolean shouldExist, TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle,
            InTupleStorePositionTracker tupleStorePositionTracker) {
        this(shouldExist, nextNodesTupleLifecycle, null, tupleStorePositionTracker);
    }

    public UnindexedIfExistsUniNode(boolean shouldExist, TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle,
            BiPredicate<A, B> filtering, InTupleStorePositionTracker tupleStorePositionTracker) {
        super(shouldExist, nextNodesTupleLifecycle, filtering != null, tupleStorePositionTracker);
        this.filtering = filtering;
    }

    @Override
    protected boolean testFiltering(UniTuple<A> leftTuple, UniTuple<B> rightTuple) {
        return filtering.test(leftTuple.factA, rightTuple.factA);
    }

}
