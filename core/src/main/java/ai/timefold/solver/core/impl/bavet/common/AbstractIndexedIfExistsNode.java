package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.index.IndexedSet;
import ai.timefold.solver.core.impl.bavet.common.index.Indexer;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory.KeysExtractor;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory.UniKeysExtractor;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

/**
 * There is a strong likelihood that any change to this class, which is not related to indexing,
 * should also be made to {@link AbstractUnindexedIfExistsNode}.
 *
 * @param <LeftTuple_>
 * @param <Right_>
 */
public abstract class AbstractIndexedIfExistsNode<LeftTuple_ extends AbstractTuple, Right_>
        extends AbstractIfExistsNode<LeftTuple_, Right_>
        implements LeftTupleLifecycle<LeftTuple_>, RightTupleLifecycle<UniTuple<Right_>> {

    private final KeysExtractor<LeftTuple_> keysExtractorLeft;
    private final UniKeysExtractor<Right_> keysExtractorRight;
    private final int inputStoreIndexLeftKeys;
    private final int inputStoreIndexLeftCounter;
    private final int inputStoreIndexRightKeys;
    private final Indexer<ExistsCounter<LeftTuple_>> indexerLeft;
    private final Indexer<UniTuple<Right_>> indexerRight;

    protected AbstractIndexedIfExistsNode(boolean shouldExist,
            KeysExtractor<LeftTuple_> keysExtractorLeft, IndexerFactory<Right_> indexerFactory,
            TupleStorePositionTracker leftTupleStorePositionTracker, TupleStorePositionTracker rightTupleStorePositionTracker,
            TupleLifecycle<LeftTuple_> nextNodesTupleLifecycle,
            boolean isFiltering) {
        super(shouldExist, leftTupleStorePositionTracker, rightTupleStorePositionTracker, nextNodesTupleLifecycle, isFiltering);
        this.keysExtractorLeft = keysExtractorLeft;
        this.keysExtractorRight = indexerFactory.buildRightKeysExtractor();
        this.inputStoreIndexLeftKeys = leftTupleStorePositionTracker.reserveNextAvailablePosition();
        this.inputStoreIndexLeftCounter = leftTupleStorePositionTracker.reserveNextAvailablePosition();
        this.inputStoreIndexRightKeys = rightTupleStorePositionTracker.reserveNextAvailablePosition();
        this.indexerLeft = indexerFactory.buildIndexer(true, ExistsCounterPositionTracker.instance());
        this.indexerRight = indexerFactory.buildIndexer(false,
                new TuplePositionTracker<>(rightTupleStorePositionTracker.reserveNextAvailablePosition()));
    }

    @Override
    public final void insertLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(inputStoreIndexLeftKeys) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(leftTuple));
        }
        var indexKeys = keysExtractorLeft.apply(leftTuple);
        leftTuple.setStore(inputStoreIndexLeftKeys, indexKeys);

        var counter = new ExistsCounter<>(leftTuple);
        indexerLeft.put(indexKeys, counter);
        updateCounterRight(leftTuple, indexKeys, counter);
        initCounterLeft(counter);
    }

    private void updateCounterRight(LeftTuple_ leftTuple, Object indexKeys, ExistsCounter<LeftTuple_> counter) {
        leftTuple.setStore(inputStoreIndexLeftCounter, counter);
        if (!isFiltering) {
            counter.countRight = indexerRight.size(indexKeys);
        } else {
            var leftHandleSet = new IndexedSet<ExistsCounterHandle<LeftTuple_>>(ExistsCounterHandlePositionTracker.left());
            indexerRight.forEach(indexKeys,
                    rightTuple -> updateCounterFromLeft(leftTuple, rightTuple, counter, leftHandleSet));
            leftTuple.setStore(inputStoreIndexLeftHandleSet, leftHandleSet);
        }
    }

    @Override
    public final void updateLeft(LeftTuple_ leftTuple) {
        var oldIndexKeys = leftTuple.getStore(inputStoreIndexLeftKeys);
        if (oldIndexKeys == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertLeft(leftTuple);
            return;
        }
        var newIndexKeys = keysExtractorLeft.apply(leftTuple);
        ExistsCounter<LeftTuple_> counter = leftTuple.getStore(inputStoreIndexLeftCounter);

        if (oldIndexKeys.equals(newIndexKeys)) {
            // No need for re-indexing because the index keys didn't change
            // The indexers contain counters in the DEAD state, to track the rightCount.
            if (!isFiltering) {
                updateUnchangedCounterLeft(counter);
            } else {
                // Call filtering for the leftTuple and rightTuple combinations again
                IndexedSet<ExistsCounterHandle<LeftTuple_>> leftHandleSet = leftTuple.getStore(inputStoreIndexLeftHandleSet);
                leftHandleSet.forEach(ExistsCounterHandle::remove);
                counter.countRight = 0;
                indexerRight.forEach(oldIndexKeys,
                        rightTuple -> updateCounterFromLeft(leftTuple, rightTuple, counter, leftHandleSet));
                updateCounterLeft(counter);
            }
        } else {
            updateIndexerLeft(oldIndexKeys, counter, leftTuple);
            counter.countRight = 0;
            leftTuple.setStore(inputStoreIndexLeftKeys, newIndexKeys);
            indexerLeft.put(newIndexKeys, counter);
            updateCounterRight(leftTuple, newIndexKeys, counter);
            updateCounterLeft(counter);
        }
    }

    @Override
    public final void retractLeft(LeftTuple_ leftTuple) {
        var indexKeys = leftTuple.removeStore(inputStoreIndexLeftKeys);
        if (indexKeys == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ExistsCounter<LeftTuple_> counter = leftTuple.getStore(inputStoreIndexLeftCounter);
        updateIndexerLeft(indexKeys, counter, leftTuple);
        killCounterLeft(counter);
    }

    private void updateIndexerLeft(Object indexKeys, ExistsCounter<LeftTuple_> counter, LeftTuple_ leftTuple) {
        indexerLeft.remove(indexKeys, counter);
        if (isFiltering) {
            IndexedSet<ExistsCounterHandle<LeftTuple_>> leftHandleSet = leftTuple.getStore(inputStoreIndexLeftHandleSet);
            leftHandleSet.forEach(ExistsCounterHandle::remove);
        }
    }

    @Override
    public final void insertRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightKeys) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(rightTuple));
        }
        var indexKeys = keysExtractorRight.apply(rightTuple);
        rightTuple.setStore(inputStoreIndexRightKeys, indexKeys);

        indexerRight.put(indexKeys, rightTuple);
        updateCounterLeft(rightTuple, indexKeys);
    }

    private void updateCounterLeft(UniTuple<Right_> rightTuple, Object indexKeys) {
        if (!isFiltering) {
            indexerLeft.forEach(indexKeys, this::incrementCounterRight);
        } else {
            var rightHandleSet = new IndexedSet<ExistsCounterHandle<LeftTuple_>>(ExistsCounterHandlePositionTracker.right());
            indexerLeft.forEach(indexKeys, counter -> updateCounterFromRight(rightTuple, counter, rightHandleSet));
            rightTuple.setStore(inputStoreIndexRightHandleSet, rightHandleSet);
        }
    }

    @Override
    public final void updateRight(UniTuple<Right_> rightTuple) {
        var oldIndexKeys = rightTuple.getStore(inputStoreIndexRightKeys);
        if (oldIndexKeys == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertRight(rightTuple);
            return;
        }
        var newIndexKeys = keysExtractorRight.apply(rightTuple);
        if (oldIndexKeys.equals(newIndexKeys)) {
            // No need for re-indexing because the index keys didn't change
            if (isFiltering) {
                var rightHandleSet = updateRightHandleSet(rightTuple);
                indexerLeft.forEach(oldIndexKeys,
                        counter -> updateCounterFromRight(rightTuple, counter, rightHandleSet));
            }
        } else {
            indexerRight.remove(oldIndexKeys, rightTuple);
            if (!isFiltering) {
                indexerLeft.forEach(oldIndexKeys, this::decrementCounterRight);
            } else {
                updateRightHandleSet(rightTuple);
            }
            rightTuple.setStore(inputStoreIndexRightKeys, newIndexKeys);
            indexerRight.put(newIndexKeys, rightTuple);
            updateCounterLeft(rightTuple, newIndexKeys);
        }
    }

    @Override
    public final void retractRight(UniTuple<Right_> rightTuple) {
        var indexKeys = rightTuple.removeStore(inputStoreIndexRightKeys);
        if (indexKeys == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        indexerRight.remove(indexKeys, rightTuple);
        if (!isFiltering) {
            indexerLeft.forEach(indexKeys, this::decrementCounterRight);
        } else {
            updateRightHandleSet(rightTuple);
        }
    }

}
