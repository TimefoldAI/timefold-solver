package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import java.util.function.Function;

import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractIndexedJoinNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexProperties;
import ai.timefold.solver.core.impl.score.stream.bavet.common.index.Indexer;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class IndexedJoinQuadNode<A, B, C, D>
        extends AbstractIndexedJoinNode<TriTuple<A, B, C>, D, QuadTuple<A, B, C, D>> {

    private final TriFunction<A, B, C, IndexProperties> mappingABC;
    private final QuadPredicate<A, B, C, D> filtering;
    private final int outputStoreSize;

    public IndexedJoinQuadNode(TriFunction<A, B, C, IndexProperties> mappingABC, Function<D, IndexProperties> mappingD,
            int inputStoreIndexABC, int inputStoreIndexEntryABC, int inputStoreIndexOutTupleListABC,
            int inputStoreIndexD, int inputStoreIndexEntryD, int inputStoreIndexOutTupleListD,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle, QuadPredicate<A, B, C, D> filtering,
            int outputStoreSize,
            int outputStoreIndexOutEntryABC, int outputStoreIndexOutEntryD,
            Indexer<TriTuple<A, B, C>> indexerABC,
            Indexer<UniTuple<D>> indexerD) {
        super(mappingD,
                inputStoreIndexABC, inputStoreIndexEntryABC, inputStoreIndexOutTupleListABC,
                inputStoreIndexD, inputStoreIndexEntryD, inputStoreIndexOutTupleListD,
                nextNodesTupleLifecycle, filtering != null,
                outputStoreIndexOutEntryABC, outputStoreIndexOutEntryD,
                indexerABC, indexerD);
        this.mappingABC = mappingABC;
        this.filtering = filtering;
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected IndexProperties createIndexPropertiesLeft(TriTuple<A, B, C> leftTuple) {
        return mappingABC.apply(leftTuple.factA, leftTuple.factB, leftTuple.factC);
    }

    @Override
    protected QuadTuple<A, B, C, D> createOutTuple(TriTuple<A, B, C> leftTuple, UniTuple<D> rightTuple) {
        return new QuadTuple<>(leftTuple.factA, leftTuple.factB, leftTuple.factC, rightTuple.factA,
                outputStoreSize);
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
