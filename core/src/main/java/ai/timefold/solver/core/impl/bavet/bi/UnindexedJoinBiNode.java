package ai.timefold.solver.core.impl.bavet.bi;

import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.bavet.common.AbstractUnindexedJoinNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.OutputStoreSizeTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class UnindexedJoinBiNode<A, B>
        extends AbstractUnindexedJoinNode<UniTuple<A>, B, BiTuple<A, B>> {

    private final BiPredicate<A, B> filtering;

    public UnindexedJoinBiNode(TupleStorePositionTracker leftTupleStorePositionTracker,
            TupleStorePositionTracker rightTupleStorePositionTracker, OutputStoreSizeTracker outputStoreSizeTracker,
            TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle, BiPredicate<A, B> filtering) {
        super(leftTupleStorePositionTracker, rightTupleStorePositionTracker, outputStoreSizeTracker, nextNodesTupleLifecycle,
                filtering != null);
        this.filtering = filtering;
    }

    @Override
    protected BiTuple<A, B> createOutTuple(UniTuple<A> leftTuple, UniTuple<B> rightTuple) {
        return new BiTuple<>(leftTuple.factA, rightTuple.factA, outputStoreSizeTracker.computeOutputStoreSize());
    }

    @Override
    protected void setOutTupleLeftFacts(BiTuple<A, B> outTuple, UniTuple<A> leftTuple) {
        outTuple.factA = leftTuple.factA;
    }

    @Override
    protected void setOutTupleRightFact(BiTuple<A, B> outTuple, UniTuple<B> rightTuple) {
        outTuple.factB = rightTuple.factA;
    }

    @Override
    protected boolean testFiltering(UniTuple<A> leftTuple, UniTuple<B> rightTuple) {
        return filtering.test(leftTuple.factA, rightTuple.factA);
    }

}
