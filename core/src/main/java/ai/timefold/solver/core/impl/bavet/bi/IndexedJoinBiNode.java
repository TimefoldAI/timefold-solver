package ai.timefold.solver.core.impl.bavet.bi;

import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.bavet.common.AbstractIndexedJoinNode;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.InOutTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class IndexedJoinBiNode<A, B> extends AbstractIndexedJoinNode<UniTuple<A>, B, BiTuple<A, B>> {

    private final BiPredicate<A, B> filtering;

    public IndexedJoinBiNode(IndexerFactory<B> indexerFactory, TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle,
            BiPredicate<A, B> filtering, InOutTupleStorePositionTracker tupleStorePositionTracker) {
        super(indexerFactory.buildUniLeftKeysExtractor(), indexerFactory, nextNodesTupleLifecycle, filtering != null,
                tupleStorePositionTracker);
        this.filtering = filtering;
    }

    @Override
    protected BiTuple<A, B> createOutTuple(UniTuple<A> leftTuple, UniTuple<B> rightTuple) {
        return new BiTuple<>(leftTuple.factA, rightTuple.factA, outputStoreSizeTracker.computeStoreSize());
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
