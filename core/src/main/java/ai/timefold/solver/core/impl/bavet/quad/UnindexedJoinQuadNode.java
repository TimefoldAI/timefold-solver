package ai.timefold.solver.core.impl.bavet.quad;

import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.impl.bavet.common.AbstractUnindexedJoinNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleStoreSizeTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class UnindexedJoinQuadNode<A, B, C, D>
        extends AbstractUnindexedJoinNode<TriTuple<A, B, C>, D, QuadTuple<A, B, C, D>> {

    private final QuadPredicate<A, B, C, D> filtering;

    public UnindexedJoinQuadNode(TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle,
            QuadPredicate<A, B, C, D> filtering, TupleStorePositionTracker leftTupleStorePositionTracker,
            TupleStorePositionTracker rightTupleStorePositionTracker, TupleStoreSizeTracker tupleStoreSizeTracker) {
        super(nextNodesTupleLifecycle, filtering != null, leftTupleStorePositionTracker, rightTupleStorePositionTracker,
                tupleStoreSizeTracker);
        this.filtering = filtering;
    }

    @Override
    protected QuadTuple<A, B, C, D> createOutTuple(TriTuple<A, B, C> leftTuple, UniTuple<D> rightTuple) {
        return new QuadTuple<>(leftTuple.factA, leftTuple.factB, leftTuple.factC, rightTuple.factA,
                tupleStoreSizeTracker.computeStoreSize());
    }

    @Override
    protected void setOutTupleLeftFacts(QuadTuple<A, B, C, D> outTuple, TriTuple<A, B, C> leftTuple) {
        outTuple.factA = leftTuple.factA;
        outTuple.factB = leftTuple.factB;
        outTuple.factC = leftTuple.factC;
    }

    @Override
    protected void setOutTupleRightFact(QuadTuple<A, B, C, D> outTuple, UniTuple<D> rightTuple) {
        outTuple.factD = rightTuple.factA;
    }

    @Override
    protected boolean testFiltering(TriTuple<A, B, C> leftTuple, UniTuple<D> rightTuple) {
        return filtering.test(leftTuple.factA, leftTuple.factB, leftTuple.factC, rightTuple.factA);
    }

}
