package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.index.Indexer;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory.KeysExtractor;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory.UniKeysExtractor;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.InTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.util.ElementAwareLinkedList;
import ai.timefold.solver.core.impl.util.ListEntry;

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
    private final int inputStoreIndexLefCompositeKey;
    private final int inputStoreIndexLeftCounterEntry;
    private final int inputStoreIndexRightCompositeKey;
    private final int inputStoreIndexRightEntry;
    private final Indexer<ExistsCounter<LeftTuple_>> indexerLeft;
    private final Indexer<UniTuple<Right_>> indexerRight;

    protected AbstractIndexedIfExistsNode(boolean shouldExist, KeysExtractor<LeftTuple_> keysExtractorLeft,
            IndexerFactory<Right_> indexerFactory, TupleLifecycle<LeftTuple_> nextNodesTupleLifecycle, boolean isFiltering,
            InTupleStorePositionTracker tupleStorePositionTracker) {
        super(shouldExist, nextNodesTupleLifecycle, isFiltering, tupleStorePositionTracker);
        this.keysExtractorLeft = keysExtractorLeft;
        this.keysExtractorRight = indexerFactory.buildRightKeysExtractor();
        this.inputStoreIndexLefCompositeKey = tupleStorePositionTracker.reserveNextLeft();
        this.inputStoreIndexLeftCounterEntry = tupleStorePositionTracker.reserveNextLeft();
        this.inputStoreIndexRightCompositeKey = tupleStorePositionTracker.reserveNextRight();
        this.inputStoreIndexRightEntry = tupleStorePositionTracker.reserveNextRight();
        this.indexerLeft = indexerFactory.buildIndexer(true);
        this.indexerRight = indexerFactory.buildIndexer(false);
    }

    @Override
    public final void insertLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(inputStoreIndexLefCompositeKey) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(leftTuple));
        }
        var compositeKey = keysExtractorLeft.apply(leftTuple);
        leftTuple.setStore(inputStoreIndexLefCompositeKey, compositeKey);

        var counter = new ExistsCounter<>(leftTuple);
        updateCounterRight(leftTuple, compositeKey, counter, indexerLeft.put(compositeKey, counter));
        initCounterLeft(counter);
    }

    private void updateCounterRight(LeftTuple_ leftTuple, Object compositeKey, ExistsCounter<LeftTuple_> counter,
            ListEntry<ExistsCounter<LeftTuple_>> counterEntry) {
        leftTuple.setStore(inputStoreIndexLeftCounterEntry, counterEntry);
        if (!isFiltering) {
            counter.countRight = indexerRight.size(compositeKey);
        } else {
            var leftTrackerList = new ElementAwareLinkedList<FilteringTracker<LeftTuple_>>();
            indexerRight.forEach(compositeKey,
                    rightTuple -> updateCounterFromLeft(counter, rightTuple, leftTrackerList));
            leftTuple.setStore(inputStoreIndexLeftTrackerList, leftTrackerList);
        }
    }

    @Override
    public final void updateLeft(LeftTuple_ leftTuple) {
        var oldCompositeKey = leftTuple.getStore(inputStoreIndexLefCompositeKey);
        if (oldCompositeKey == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertLeft(leftTuple);
            return;
        }
        var newCompositeKey = keysExtractorLeft.apply(leftTuple);
        ListEntry<ExistsCounter<LeftTuple_>> counterEntry = leftTuple.getStore(inputStoreIndexLeftCounterEntry);
        var counter = counterEntry.getElement();

        if (oldCompositeKey.equals(newCompositeKey)) {
            // No need for re-indexing because the index keys didn't change
            // The indexers contain counters in the DEAD state, to track the rightCount.
            if (!isFiltering) {
                updateUnchangedCounterLeft(counter);
            } else {
                // Call filtering for the leftTuple and rightTuple combinations again
                ElementAwareLinkedList<FilteringTracker<LeftTuple_>> leftTrackerList =
                        leftTuple.getStore(inputStoreIndexLeftTrackerList);
                leftTrackerList.clear(FilteringTracker::removeByLeft);
                counter.countRight = 0;
                indexerRight.forEach(oldCompositeKey,
                        rightTuple -> updateCounterFromLeft(counter, rightTuple, leftTrackerList));
                updateCounterLeft(counter);
            }
        } else {
            updateIndexerLeft(oldCompositeKey, counterEntry, leftTuple);
            counter.countRight = 0;
            leftTuple.setStore(inputStoreIndexLefCompositeKey, newCompositeKey);
            updateCounterRight(leftTuple, newCompositeKey, counter, indexerLeft.put(newCompositeKey, counter));
            updateCounterLeft(counter);
        }
    }

    @Override
    public final void retractLeft(LeftTuple_ leftTuple) {
        var compositeKey = leftTuple.removeStore(inputStoreIndexLefCompositeKey);
        if (compositeKey == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ListEntry<ExistsCounter<LeftTuple_>> counterEntry = leftTuple.getStore(inputStoreIndexLeftCounterEntry);
        updateIndexerLeft(compositeKey, counterEntry, leftTuple);
        killCounterLeft(counterEntry.getElement());
    }

    private void updateIndexerLeft(Object compositeKey, ListEntry<ExistsCounter<LeftTuple_>> counterEntry,
            LeftTuple_ leftTuple) {
        indexerLeft.remove(compositeKey, counterEntry);
        if (isFiltering) {
            ElementAwareLinkedList<FilteringTracker<LeftTuple_>> leftTrackerList =
                    leftTuple.getStore(inputStoreIndexLeftTrackerList);
            leftTrackerList.clear(FilteringTracker::removeByLeft);
        }
    }

    @Override
    public final void insertRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightCompositeKey) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(rightTuple));
        }
        var compositeKey = keysExtractorRight.apply(rightTuple);
        rightTuple.setStore(inputStoreIndexRightCompositeKey, compositeKey);
        rightTuple.setStore(inputStoreIndexRightEntry, indexerRight.put(compositeKey, rightTuple));
        updateCounterLeft(rightTuple, compositeKey);
    }

    private void updateCounterLeft(UniTuple<Right_> rightTuple, Object compositeKey) {
        if (!isFiltering) {
            indexerLeft.forEach(compositeKey, this::incrementCounterRight);
        } else {
            var rightTrackerList = new ElementAwareLinkedList<FilteringTracker<LeftTuple_>>();
            indexerLeft.forEach(compositeKey, counter -> updateCounterFromRight(counter, rightTuple, rightTrackerList));
            rightTuple.setStore(inputStoreIndexRightTrackerList, rightTrackerList);
        }
    }

    @Override
    public final void updateRight(UniTuple<Right_> rightTuple) {
        var oldCompositeKey = rightTuple.getStore(inputStoreIndexRightCompositeKey);
        if (oldCompositeKey == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertRight(rightTuple);
            return;
        }
        var newCompositeKey = keysExtractorRight.apply(rightTuple);
        if (oldCompositeKey.equals(newCompositeKey)) {
            // No need for re-indexing because the index keys didn't change
            if (isFiltering) {
                var rightTrackerList = clearRightTrackerList(rightTuple);
                indexerLeft.forEach(oldCompositeKey,
                        counter -> updateCounterFromRight(counter, rightTuple, rightTrackerList));
            }
        } else {
            indexerRight.remove(oldCompositeKey, rightTuple.getStore(inputStoreIndexRightEntry));
            if (!isFiltering) {
                indexerLeft.forEach(oldCompositeKey, this::decrementCounterRight);
            } else {
                clearRightTrackerList(rightTuple);
            }
            rightTuple.setStore(inputStoreIndexRightCompositeKey, newCompositeKey);
            rightTuple.setStore(inputStoreIndexRightEntry, indexerRight.put(newCompositeKey, rightTuple));
            updateCounterLeft(rightTuple, newCompositeKey);
        }
    }

    @Override
    public final void retractRight(UniTuple<Right_> rightTuple) {
        var compositeKey = rightTuple.removeStore(inputStoreIndexRightCompositeKey);
        if (compositeKey == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        indexerRight.remove(compositeKey, rightTuple.removeStore(inputStoreIndexRightEntry));
        if (!isFiltering) {
            indexerLeft.forEach(compositeKey, this::decrementCounterRight);
        } else {
            clearRightTrackerList(rightTuple);
        }
    }

}
