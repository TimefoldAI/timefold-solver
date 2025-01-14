package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractIndexedIfExistsNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ExistsCounter;
import ai.timefold.solver.core.impl.score.stream.bavet.common.index.Indexer;
import ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexerFactory.TriMapping;
import ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexerFactory.UniMapping;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class IndexedIfExistsTriNode<A, B, C, D> extends AbstractIndexedIfExistsNode<TriTuple<A, B, C>, D> {

    private final QuadPredicate<A, B, C, D> filtering;

    public IndexedIfExistsTriNode(boolean shouldExist,
            TriMapping<A, B, C> mappingABC, UniMapping<D> mappingD,
            int inputStoreIndexLeftProperties, int inputStoreIndexLeftCounterEntry, int inputStoreIndexRightProperties,
            int inputStoreIndexRightEntry,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle,
            Indexer<ExistsCounter<TriTuple<A, B, C>>> indexerABC, Indexer<UniTuple<D>> indexerD) {
        this(shouldExist, mappingABC, mappingD,
                inputStoreIndexLeftProperties, inputStoreIndexLeftCounterEntry, -1, inputStoreIndexRightProperties,
                inputStoreIndexRightEntry, -1,
                nextNodesTupleLifecycle, indexerABC, indexerD,
                null);
    }

    public IndexedIfExistsTriNode(boolean shouldExist,
            TriMapping<A, B, C> mappingABC, UniMapping<D> mappingD,
            int inputStoreIndexLeftProperties, int inputStoreIndexLeftCounterEntry, int inputStoreIndexLeftTrackerList,
            int inputStoreIndexRightProperties, int inputStoreIndexRightEntry, int inputStoreIndexRightTrackerList,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle,
            Indexer<ExistsCounter<TriTuple<A, B, C>>> indexerABC, Indexer<UniTuple<D>> indexerD,
            QuadPredicate<A, B, C, D> filtering) {
        super(shouldExist, mappingABC, mappingD,
                inputStoreIndexLeftProperties, inputStoreIndexLeftCounterEntry, inputStoreIndexLeftTrackerList,
                inputStoreIndexRightProperties, inputStoreIndexRightEntry, inputStoreIndexRightTrackerList,
                nextNodesTupleLifecycle, indexerABC, indexerD,
                filtering != null);
        this.filtering = filtering;
    }

    @Override
    protected boolean testFiltering(TriTuple<A, B, C> leftTuple, UniTuple<D> rightTuple) {
        return filtering.test(leftTuple.factA, leftTuple.factB, leftTuple.factC, rightTuple.factA);
    }

}
