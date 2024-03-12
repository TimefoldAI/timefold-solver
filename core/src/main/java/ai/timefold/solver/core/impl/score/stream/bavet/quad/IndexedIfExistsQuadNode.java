package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import java.util.function.Function;

import ai.timefold.solver.core.api.function.PentaPredicate;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractIndexedIfExistsNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ExistsCounter;
import ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexProperties;
import ai.timefold.solver.core.impl.score.stream.bavet.common.index.Indexer;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class IndexedIfExistsQuadNode<A, B, C, D, E> extends AbstractIndexedIfExistsNode<QuadTuple<A, B, C, D>, E> {

    private final QuadFunction<A, B, C, D, IndexProperties> mappingABCD;
    private final PentaPredicate<A, B, C, D, E> filtering;

    public IndexedIfExistsQuadNode(boolean shouldExist,
            QuadFunction<A, B, C, D, IndexProperties> mappingABCD, Function<E, IndexProperties> mappingE,
            int inputStoreIndexLeftProperties, int inputStoreIndexLeftCounterEntry, int inputStoreIndexRightProperties,
            int inputStoreIndexRightEntry,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle,
            Indexer<ExistsCounter<QuadTuple<A, B, C, D>>> indexerABCD, Indexer<UniTuple<E>> indexerE) {
        this(shouldExist, mappingABCD, mappingE,
                inputStoreIndexLeftProperties, inputStoreIndexLeftCounterEntry, -1, inputStoreIndexRightProperties,
                inputStoreIndexRightEntry, -1,
                nextNodesTupleLifecycle, indexerABCD, indexerE,
                null);
    }

    public IndexedIfExistsQuadNode(boolean shouldExist,
            QuadFunction<A, B, C, D, IndexProperties> mappingABCD, Function<E, IndexProperties> mappingE,
            int inputStoreIndexLeftProperties, int inputStoreIndexLeftCounterEntry, int inputStoreIndexLeftTrackerList,
            int inputStoreIndexRightProperties, int inputStoreIndexRightEntry, int inputStoreIndexRightTrackerList,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle,
            Indexer<ExistsCounter<QuadTuple<A, B, C, D>>> indexerABCD, Indexer<UniTuple<E>> indexerE,
            PentaPredicate<A, B, C, D, E> filtering) {
        super(shouldExist, mappingE,
                inputStoreIndexLeftProperties, inputStoreIndexLeftCounterEntry, inputStoreIndexLeftTrackerList,
                inputStoreIndexRightProperties, inputStoreIndexRightEntry, inputStoreIndexRightTrackerList,
                nextNodesTupleLifecycle, indexerABCD, indexerE,
                filtering != null);
        this.mappingABCD = mappingABCD;
        this.filtering = filtering;
    }

    @Override
    protected IndexProperties createIndexProperties(QuadTuple<A, B, C, D> leftTuple) {
        return mappingABCD.apply(leftTuple.factA, leftTuple.factB, leftTuple.factC, leftTuple.factD);
    }

    @Override
    protected boolean testFiltering(QuadTuple<A, B, C, D> leftTuple, UniTuple<E> rightTuple) {
        return filtering.test(leftTuple.factA, leftTuple.factB, leftTuple.factC, leftTuple.factD,
                rightTuple.factA);
    }

}
