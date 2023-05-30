package ai.timefold.solver.constraint.streams.bavet.bi;

import java.util.function.BiPredicate;
import java.util.function.Function;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractIndexedJoinNode;
import ai.timefold.solver.constraint.streams.bavet.common.index.IndexProperties;
import ai.timefold.solver.constraint.streams.bavet.common.index.Indexer;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.BiTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;

final class IndexedJoinBiNode<A, B> extends AbstractIndexedJoinNode<UniTuple<A>, B, BiTuple<A, B>> {

    private final Function<A, IndexProperties> mappingA;
    private final BiPredicate<A, B> filtering;
    private final int outputStoreSize;

    public IndexedJoinBiNode(Function<A, IndexProperties> mappingA, Function<B, IndexProperties> mappingB,
            int inputStoreIndexA, int inputStoreIndexEntryA, int inputStoreIndexOutTupleListA,
            int inputStoreIndexB, int inputStoreIndexEntryB, int inputStoreIndexOutTupleListB,
            TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle, BiPredicate<A, B> filtering,
            int outputStoreSize,
            int outputStoreIndexOutEntryA, int outputStoreIndexOutEntryB,
            Indexer<UniTuple<A>> indexerA,
            Indexer<UniTuple<B>> indexerB) {
        super(mappingB,
                inputStoreIndexA, inputStoreIndexEntryA, inputStoreIndexOutTupleListA,
                inputStoreIndexB, inputStoreIndexEntryB, inputStoreIndexOutTupleListB,
                nextNodesTupleLifecycle, filtering != null,
                outputStoreIndexOutEntryA, outputStoreIndexOutEntryB,
                indexerA, indexerB);
        this.mappingA = mappingA;
        this.filtering = filtering;
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected IndexProperties createIndexPropertiesLeft(UniTuple<A> leftTuple) {
        return mappingA.apply(leftTuple.factA);
    }

    @Override
    protected BiTuple<A, B> createOutTuple(UniTuple<A> leftTuple, UniTuple<B> rightTuple) {
        return new BiTuple<>(leftTuple.factA, rightTuple.factA, outputStoreSize);
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
