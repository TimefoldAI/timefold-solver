package ai.timefold.solver.core.impl.bavet.tri;

import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.impl.bavet.common.AbstractIndexedIfExistsNode;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class IndexedIfExistsTriNode<A, B, C, D> extends AbstractIndexedIfExistsNode<TriTuple<A, B, C>, D> {

    private final QuadPredicate<A, B, C, D> filtering;

    public IndexedIfExistsTriNode(boolean shouldExist, IndexerFactory<D> indexerFactory,
            int inputStoreIndexLeftKeys, int inputStoreIndexLeftCounterEntry,
            int inputStoreIndexRightKeys, int inputStoreIndexRightEntry,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle) {
        this(shouldExist, indexerFactory,
                inputStoreIndexLeftKeys, inputStoreIndexLeftCounterEntry, -1,
                inputStoreIndexRightKeys, inputStoreIndexRightEntry, -1,
                nextNodesTupleLifecycle, null);
    }

    public IndexedIfExistsTriNode(boolean shouldExist, IndexerFactory<D> indexerFactory,
            int inputStoreIndexLeftKeys, int inputStoreIndexLeftCounterEntry, int inputStoreIndexLeftTrackerList,
            int inputStoreIndexRightKeys, int inputStoreIndexRightEntry, int inputStoreIndexRightTrackerList,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle, QuadPredicate<A, B, C, D> filtering) {
        super(shouldExist, indexerFactory.buildTriLeftKeysExtractor(), indexerFactory,
                inputStoreIndexLeftKeys, inputStoreIndexLeftCounterEntry, inputStoreIndexLeftTrackerList,
                inputStoreIndexRightKeys, inputStoreIndexRightEntry, inputStoreIndexRightTrackerList,
                nextNodesTupleLifecycle, filtering != null);
        this.filtering = filtering;
    }

    @Override
    protected boolean testFiltering(TriTuple<A, B, C> leftTuple, UniTuple<D> rightTuple) {
        return filtering.test(leftTuple.factA, leftTuple.factB, leftTuple.factC, rightTuple.factA);
    }

}
