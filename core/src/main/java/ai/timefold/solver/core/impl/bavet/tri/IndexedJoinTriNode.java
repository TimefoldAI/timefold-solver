package ai.timefold.solver.core.impl.bavet.tri;

import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.impl.bavet.common.AbstractIndexedJoinNode;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class IndexedJoinTriNode<A, B, C>
        extends AbstractIndexedJoinNode<BiTuple<A, B>, C, TriTuple<A, B, C>> {

    private final TriPredicate<A, B, C> filtering;
    private final int outputStoreSize;

    public IndexedJoinTriNode(IndexerFactory<C> indexerFactory,
            int inputStoreIndexAB, int inputStoreIndexEntryAB, int inputStoreIndexOutTupleListAB,
            int inputStoreIndexC, int inputStoreIndexEntryC, int inputStoreIndexOutTupleListC,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle, TriPredicate<A, B, C> filtering,
            int outputStoreSize, int outputStoreIndexOutEntryAB, int outputStoreIndexOutEntryC) {
        super(indexerFactory.buildBiLeftKeysExtractor(), indexerFactory,
                inputStoreIndexAB, inputStoreIndexEntryAB, inputStoreIndexOutTupleListAB,
                inputStoreIndexC, inputStoreIndexEntryC, inputStoreIndexOutTupleListC,
                nextNodesTupleLifecycle, filtering != null,
                outputStoreIndexOutEntryAB, outputStoreIndexOutEntryC);
        this.filtering = filtering;
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected TriTuple<A, B, C> createOutTuple(BiTuple<A, B> leftTuple, UniTuple<C> rightTuple) {
        return new TriTuple<>(leftTuple.factA, leftTuple.factB, rightTuple.factA, outputStoreSize);
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
