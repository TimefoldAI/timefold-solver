package ai.timefold.solver.core.impl.bavet.quad;

import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.impl.bavet.common.AbstractUnindexedJoinNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.InOutTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class UnindexedJoinQuadNode<A, B, C, D>
        extends AbstractUnindexedJoinNode<TriTuple<A, B, C>, D, QuadTuple<A, B, C, D>> {

    private final QuadPredicate<A, B, C, D> filtering;

    public UnindexedJoinQuadNode(TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle,
            QuadPredicate<A, B, C, D> filtering, InOutTupleStorePositionTracker tupleStorePositionTracker) {
        super(nextNodesTupleLifecycle, filtering != null, tupleStorePositionTracker);
        this.filtering = filtering;
    }

    @Override
    protected QuadTuple<A, B, C, D> createOutTuple(TriTuple<A, B, C> leftTuple, UniTuple<D> rightTuple) {
        return QuadTuple.of(leftTuple.getA(), leftTuple.getB(), leftTuple.getC(), rightTuple.getA(),
                outputStoreSizeTracker.computeStoreSize());
    }

    @Override
    protected void setOutTupleLeftFacts(QuadTuple<A, B, C, D> outTuple, TriTuple<A, B, C> leftTuple) {
        outTuple.setA(leftTuple.getA());
        outTuple.setB(leftTuple.getB());
        outTuple.setC(leftTuple.getC());
    }

    @Override
    protected void setOutTupleRightFact(QuadTuple<A, B, C, D> outTuple, UniTuple<D> rightTuple) {
        outTuple.setD(rightTuple.getA());
    }

    @Override
    protected boolean testFiltering(TriTuple<A, B, C> leftTuple, UniTuple<D> rightTuple) {
        return filtering.test(leftTuple.getA(), leftTuple.getB(), leftTuple.getC(), rightTuple.getA());
    }

}
