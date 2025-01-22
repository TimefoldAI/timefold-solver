package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.index.Indexer;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory.KeysExtractor;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory.UniKeysExtractor;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.util.ElementAwareList;
import ai.timefold.solver.core.impl.util.ElementAwareListEntry;

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
    private final int inputStoreIndexLeftCounterEntry;
    private final int inputStoreIndexRightKeys;
    private final int inputStoreIndexRightEntry;
    private final Indexer<ExistsCounter<LeftTuple_>> indexerLeft;
    private final Indexer<UniTuple<Right_>> indexerRight;

    protected AbstractIndexedIfExistsNode(boolean shouldExist,
            KeysExtractor<LeftTuple_> keysExtractorLeft, IndexerFactory<Right_> indexerFactory,
            int inputStoreIndexLeftKeys, int inputStoreIndexLeftCounterEntry, int inputStoreIndexLeftTrackerList,
            int inputStoreIndexRightKeys, int inputStoreIndexRightEntry, int inputStoreIndexRightTrackerList,
            TupleLifecycle<LeftTuple_> nextNodesTupleLifecycle,
            boolean isFiltering) {
        super(shouldExist,
                inputStoreIndexLeftTrackerList, inputStoreIndexRightTrackerList,
                nextNodesTupleLifecycle, isFiltering);
        this.keysExtractorLeft = keysExtractorLeft;
        this.keysExtractorRight = indexerFactory.buildRightKeysExtractor();
        this.inputStoreIndexLeftKeys = inputStoreIndexLeftKeys;
        this.inputStoreIndexLeftCounterEntry = inputStoreIndexLeftCounterEntry;
        this.inputStoreIndexRightKeys = inputStoreIndexRightKeys;
        this.inputStoreIndexRightEntry = inputStoreIndexRightEntry;
        this.indexerLeft = indexerFactory.buildIndexer(true);
        this.indexerRight = indexerFactory.buildIndexer(false);
    }

    @Override
    public final void insertLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(inputStoreIndexLeftKeys) != null) {
            throw new IllegalStateException("Impossible state: the input for the tuple (" + leftTuple
                    + ") was already added in the tupleStore.");
        }
        var indexKeys = keysExtractorLeft.apply(leftTuple);
        leftTuple.setStore(inputStoreIndexLeftKeys, indexKeys);

        var counter = new ExistsCounter<>(leftTuple);
        var counterEntry = indexerLeft.put(indexKeys, counter);
        updateCounterRight(leftTuple, indexKeys, counter, counterEntry);
        initCounterLeft(counter);
    }

    private void updateCounterRight(LeftTuple_ leftTuple, Object indexKeys, ExistsCounter<LeftTuple_> counter,
            ElementAwareListEntry<ExistsCounter<LeftTuple_>> counterEntry) {
        leftTuple.setStore(inputStoreIndexLeftCounterEntry, counterEntry);
        if (!isFiltering) {
            counter.countRight = indexerRight.size(indexKeys);
        } else {
            var leftTrackerList = new ElementAwareList<FilteringTracker<LeftTuple_>>();
            indexerRight.forEach(indexKeys,
                    rightTuple -> updateCounterFromLeft(leftTuple, rightTuple, counter, leftTrackerList));
            leftTuple.setStore(inputStoreIndexLeftTrackerList, leftTrackerList);
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
        ElementAwareListEntry<ExistsCounter<LeftTuple_>> counterEntry = leftTuple.getStore(inputStoreIndexLeftCounterEntry);
        var counter = counterEntry.getElement();

        if (oldIndexKeys.equals(newIndexKeys)) {
            // No need for re-indexing because the index keys didn't change
            // The indexers contain counters in the DEAD state, to track the rightCount.
            if (!isFiltering) {
                updateUnchangedCounterLeft(counter);
            } else {
                // Call filtering for the leftTuple and rightTuple combinations again
                ElementAwareList<FilteringTracker<LeftTuple_>> leftTrackerList =
                        leftTuple.getStore(inputStoreIndexLeftTrackerList);
                leftTrackerList.forEach(FilteringTracker::remove);
                counter.countRight = 0;
                indexerRight.forEach(oldIndexKeys,
                        rightTuple -> updateCounterFromLeft(leftTuple, rightTuple, counter, leftTrackerList));
                updateCounterLeft(counter);
            }
        } else {
            updateIndexerLeft(oldIndexKeys, counterEntry, leftTuple);
            counter.countRight = 0;
            leftTuple.setStore(inputStoreIndexLeftKeys, newIndexKeys);
            updateCounterRight(leftTuple, newIndexKeys, counter, indexerLeft.put(newIndexKeys, counter));
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
        ElementAwareListEntry<ExistsCounter<LeftTuple_>> counterEntry = leftTuple.getStore(inputStoreIndexLeftCounterEntry);
        var counter = counterEntry.getElement();
        updateIndexerLeft(indexKeys, counterEntry, leftTuple);
        killCounterLeft(counter);
    }

    private void updateIndexerLeft(Object indexKeys, ElementAwareListEntry<ExistsCounter<LeftTuple_>> counterEntry,
            LeftTuple_ leftTuple) {
        indexerLeft.remove(indexKeys, counterEntry);
        if (isFiltering) {
            ElementAwareList<FilteringTracker<LeftTuple_>> leftTrackerList = leftTuple.getStore(inputStoreIndexLeftTrackerList);
            leftTrackerList.forEach(FilteringTracker::remove);
        }
    }

    @Override
    public final void insertRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightKeys) != null) {
            throw new IllegalStateException("Impossible state: the input for the tuple (" + rightTuple
                    + ") was already added in the tupleStore.");
        }
        var indexKeys = keysExtractorRight.apply(rightTuple);
        rightTuple.setStore(inputStoreIndexRightKeys, indexKeys);

        var rightEntry = indexerRight.put(indexKeys, rightTuple);
        rightTuple.setStore(inputStoreIndexRightEntry, rightEntry);
        updateCounterLeft(rightTuple, indexKeys);
    }

    private void updateCounterLeft(UniTuple<Right_> rightTuple, Object indexKeys) {
        if (!isFiltering) {
            indexerLeft.forEach(indexKeys, this::incrementCounterRight);
        } else {
            var rightTrackerList = new ElementAwareList<FilteringTracker<LeftTuple_>>();
            indexerLeft.forEach(indexKeys, counter -> updateCounterFromRight(rightTuple, counter, rightTrackerList));
            rightTuple.setStore(inputStoreIndexRightTrackerList, rightTrackerList);
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
                var rightTrackerList = updateRightTrackerList(rightTuple);
                indexerLeft.forEach(oldIndexKeys,
                        counter -> updateCounterFromRight(rightTuple, counter, rightTrackerList));
            }
        } else {
            ElementAwareListEntry<UniTuple<Right_>> rightEntry = rightTuple.getStore(inputStoreIndexRightEntry);
            indexerRight.remove(oldIndexKeys, rightEntry);
            if (!isFiltering) {
                indexerLeft.forEach(oldIndexKeys, this::decrementCounterRight);
            } else {
                updateRightTrackerList(rightTuple);
            }
            rightTuple.setStore(inputStoreIndexRightKeys, newIndexKeys);
            rightEntry = indexerRight.put(newIndexKeys, rightTuple);
            rightTuple.setStore(inputStoreIndexRightEntry, rightEntry);
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
        ElementAwareListEntry<UniTuple<Right_>> rightEntry = rightTuple.removeStore(inputStoreIndexRightEntry);
        indexerRight.remove(indexKeys, rightEntry);
        if (!isFiltering) {
            indexerLeft.forEach(indexKeys, this::decrementCounterRight);
        } else {
            updateRightTrackerList(rightTuple);
        }
    }

}
