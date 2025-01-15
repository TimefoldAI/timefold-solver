package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractIndexedIfExistsNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class IndexedIfExistsUniNode<A, B> extends AbstractIndexedIfExistsNode<UniTuple<A>, B> {

    private final BiPredicate<A, B> filtering;

    public IndexedIfExistsUniNode(boolean shouldExist, IndexerFactory<B> indexerFactory,
            int inputStoreIndexLeftKeys, int inputStoreIndexLeftCounterEntry,
            int inputStoreIndexRightKeys, int inputStoreIndexRightEntry,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle) {
        this(shouldExist, indexerFactory,
                inputStoreIndexLeftKeys, inputStoreIndexLeftCounterEntry, -1,
                inputStoreIndexRightKeys, inputStoreIndexRightEntry, -1,
                nextNodesTupleLifecycle, null);
    }

    public IndexedIfExistsUniNode(boolean shouldExist, IndexerFactory<B> indexerFactory,
            int inputStoreIndexLeftKeys, int inputStoreIndexLeftCounterEntry, int inputStoreIndexLeftTrackerList,
            int inputStoreIndexRightKeys, int inputStoreIndexRightEntry, int inputStoreIndexRightTrackerList,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle, BiPredicate<A, B> filtering) {
        super(shouldExist, indexerFactory.buildUniLeftKeysExtractor(), indexerFactory,
                inputStoreIndexLeftKeys, inputStoreIndexLeftCounterEntry, inputStoreIndexLeftTrackerList,
                inputStoreIndexRightKeys, inputStoreIndexRightEntry, inputStoreIndexRightTrackerList,
                nextNodesTupleLifecycle, filtering != null);
        this.filtering = filtering;
    }

    @Override
    protected boolean testFiltering(UniTuple<A> leftTuple, UniTuple<B> rightTuple) {
        return filtering.test(leftTuple.factA, rightTuple.factA);
    }

}
