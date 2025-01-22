package ai.timefold.solver.core.impl.bavet.bi;

import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.impl.bavet.common.AbstractIndexedIfExistsNode;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class IndexedIfExistsBiNode<A, B, C> extends AbstractIndexedIfExistsNode<BiTuple<A, B>, C> {

    private final TriPredicate<A, B, C> filtering;

    public IndexedIfExistsBiNode(boolean shouldExist, IndexerFactory<C> indexerFactory,
            int inputStoreIndexLeftKeys, int inputStoreIndexLeftCounterEntry,
            int inputStoreIndexRightKeys, int inputStoreIndexRightEntry,
            TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle) {
        this(shouldExist, indexerFactory,
                inputStoreIndexLeftKeys, inputStoreIndexLeftCounterEntry, -1,
                inputStoreIndexRightKeys, inputStoreIndexRightEntry, -1,
                nextNodesTupleLifecycle, null);
    }

    public IndexedIfExistsBiNode(boolean shouldExist, IndexerFactory<C> indexerFactory,
            int inputStoreIndexLeftKeys, int inputStoreIndexLeftCounterEntry, int inputStoreIndexLeftTrackerList,
            int inputStoreIndexRightKeys, int inputStoreIndexRightEntry, int inputStoreIndexRightTrackerList,
            TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle, TriPredicate<A, B, C> filtering) {
        super(shouldExist, indexerFactory.buildBiLeftKeysExtractor(), indexerFactory,
                inputStoreIndexLeftKeys, inputStoreIndexLeftCounterEntry, inputStoreIndexLeftTrackerList,
                inputStoreIndexRightKeys, inputStoreIndexRightEntry, inputStoreIndexRightTrackerList,
                nextNodesTupleLifecycle, filtering != null);
        this.filtering = filtering;
    }

    @Override
    protected boolean testFiltering(BiTuple<A, B> leftTuple, UniTuple<C> rightTuple) {
        return filtering.test(leftTuple.factA, leftTuple.factB, rightTuple.factA);
    }

}
