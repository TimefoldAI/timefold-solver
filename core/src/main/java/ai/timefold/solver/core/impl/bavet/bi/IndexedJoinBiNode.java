package ai.timefold.solver.core.impl.bavet.bi;

import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.bavet.common.AbstractIndexedJoinNode;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleStoreSizeTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class IndexedJoinBiNode<A, B> extends AbstractIndexedJoinNode<UniTuple<A>, B, BiTuple<A, B>> {

    private final BiPredicate<A, B> filtering;

    public IndexedJoinBiNode(IndexerFactory<B> indexerFactory,
            int inputStoreIndexA, int inputStoreIndexEntryA, int inputStoreIndexOutTupleListA,
            int inputStoreIndexB, int inputStoreIndexEntryB, int inputStoreIndexOutTupleListB,
            TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle, BiPredicate<A, B> filtering,
            TupleStoreSizeTracker tupleStoreSizeTracker) {
        super(indexerFactory.buildUniLeftKeysExtractor(), indexerFactory,
                inputStoreIndexA, inputStoreIndexEntryA, inputStoreIndexOutTupleListA,
                inputStoreIndexB, inputStoreIndexEntryB, inputStoreIndexOutTupleListB,
                nextNodesTupleLifecycle, filtering != null, tupleStoreSizeTracker);
        this.filtering = filtering;
    }

    @Override
    protected BiTuple<A, B> createOutTuple(UniTuple<A> leftTuple, UniTuple<B> rightTuple) {
        return new BiTuple<>(leftTuple.factA, rightTuple.factA, tupleStoreSizeTracker.computeStoreSize());
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
