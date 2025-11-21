package ai.timefold.solver.core.impl.bavet.tri;

import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.impl.bavet.common.AbstractUnindexedJoinNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleStoreSizeTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class UnindexedJoinTriNode<A, B, C>
        extends AbstractUnindexedJoinNode<BiTuple<A, B>, C, TriTuple<A, B, C>> {

    private final TriPredicate<A, B, C> filtering;

    public UnindexedJoinTriNode(TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle, TriPredicate<A, B, C> filtering,
            TupleStorePositionTracker leftTupleStorePositionTracker, TupleStorePositionTracker rightTupleStorePositionTracker,
            TupleStoreSizeTracker tupleStoreSizeTracker) {
        super(nextNodesTupleLifecycle, filtering != null, leftTupleStorePositionTracker, rightTupleStorePositionTracker,
                tupleStoreSizeTracker);
        this.filtering = filtering;
    }

    @Override
    protected TriTuple<A, B, C> createOutTuple(BiTuple<A, B> leftTuple, UniTuple<C> rightTuple) {
        return new TriTuple<>(leftTuple.factA, leftTuple.factB, rightTuple.factA, tupleStoreSizeTracker.computeStoreSize());
    }

    @Override
    protected void setOutTupleLeftFacts(TriTuple<A, B, C> outTuple, BiTuple<A, B> leftTuple) {
        outTuple.factA = leftTuple.factA;
        outTuple.factB = leftTuple.factB;
    }

    @Override
    protected void setOutTupleRightFact(TriTuple<A, B, C> outTuple, UniTuple<C> rightTuple) {
        outTuple.factC = rightTuple.factA;
    }

    @Override
    protected boolean testFiltering(BiTuple<A, B> leftTuple, UniTuple<C> rightTuple) {
        return filtering.test(leftTuple.factA, leftTuple.factB, rightTuple.factA);
    }

}
