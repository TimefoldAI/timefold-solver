package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractIndexedJoinNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.index.Indexer;
import ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexerFactory.BiMapping;
import ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexerFactory.UniMapping;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class IndexedJoinTriNode<A, B, C>
        extends AbstractIndexedJoinNode<BiTuple<A, B>, C, TriTuple<A, B, C>> {

    private final BiMapping<A, B> mappingAB;
    private final TriPredicate<A, B, C> filtering;
    private final int outputStoreSize;

    public IndexedJoinTriNode(BiMapping<A, B> mappingAB, UniMapping<C> mappingC,
            int inputStoreIndexAB, int inputStoreIndexEntryAB, int inputStoreIndexOutTupleListAB,
            int inputStoreIndexC, int inputStoreIndexEntryC, int inputStoreIndexOutTupleListC,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle, TriPredicate<A, B, C> filtering,
            int outputStoreSize, int outputStoreIndexOutEntryAB, int outputStoreIndexOutEntryC,
            Indexer<BiTuple<A, B>> indexerAB,
            Indexer<UniTuple<C>> indexerC) {
        super(mappingC,
                inputStoreIndexAB, inputStoreIndexEntryAB, inputStoreIndexOutTupleListAB,
                inputStoreIndexC, inputStoreIndexEntryC, inputStoreIndexOutTupleListC,
                nextNodesTupleLifecycle, filtering != null,
                outputStoreIndexOutEntryAB, outputStoreIndexOutEntryC,
                indexerAB, indexerC);
        this.mappingAB = mappingAB;
        this.filtering = filtering;
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected Object createIndexPropertiesLeft(BiTuple<A, B> leftTuple) {
        return mappingAB.apply(leftTuple.factA, leftTuple.factB);
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
