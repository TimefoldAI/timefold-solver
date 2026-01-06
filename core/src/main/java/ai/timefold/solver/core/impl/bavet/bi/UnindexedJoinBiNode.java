package ai.timefold.solver.core.impl.bavet.bi;

import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.bavet.common.AbstractUnindexedJoinNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.InOutTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class UnindexedJoinBiNode<A, B>
        extends AbstractUnindexedJoinNode<UniTuple<A>, B, BiTuple<A, B>> {

    private final BiPredicate<A, B> filtering;

    public UnindexedJoinBiNode(TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle, BiPredicate<A, B> filtering,
            InOutTupleStorePositionTracker tupleStorePositionTracker) {
        super(nextNodesTupleLifecycle, filtering != null, tupleStorePositionTracker);
        this.filtering = filtering;
    }

    @Override
    protected BiTuple<A, B> createOutTuple(UniTuple<A> leftTuple, UniTuple<B> rightTuple) {
        return BiTuple.of(leftTuple.getA(), rightTuple.getA(), outputStoreSizeTracker.computeStoreSize());
    }

    @Override
    protected void setOutTupleLeftFacts(BiTuple<A, B> outTuple, UniTuple<A> leftTuple) {
        outTuple.setA(leftTuple.getA());
    }

    @Override
    protected void setOutTupleRightFact(BiTuple<A, B> outTuple, UniTuple<B> rightTuple) {
        outTuple.setB(rightTuple.getA());
    }

    @Override
    protected boolean testFiltering(UniTuple<A> leftTuple, UniTuple<B> rightTuple) {
        return filtering.test(leftTuple.getA(), rightTuple.getA());
    }

}
