package ai.timefold.solver.core.impl.score.stream.bavet.common;

import java.util.function.Function;

import ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexProperties;
import ai.timefold.solver.core.impl.score.stream.bavet.common.index.Indexer;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;
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

    private final Function<Right_, IndexProperties> mappingRight;
    private final int inputStoreIndexLeftProperties;
    private final int inputStoreIndexLeftCounterEntry;
    private final int inputStoreIndexRightProperties;
    private final int inputStoreIndexRightEntry;
    private final Indexer<ExistsCounter<LeftTuple_>> indexerLeft;
    private final Indexer<UniTuple<Right_>> indexerRight;

    protected AbstractIndexedIfExistsNode(boolean shouldExist,
            Function<Right_, IndexProperties> mappingRight,
            int inputStoreIndexLeftProperties, int inputStoreIndexLeftCounterEntry, int inputStoreIndexLeftTrackerList,
            int inputStoreIndexRightProperties, int inputStoreIndexRightEntry, int inputStoreIndexRightTrackerList,
            TupleLifecycle<LeftTuple_> nextNodesTupleLifecycle,
            Indexer<ExistsCounter<LeftTuple_>> indexerLeft,
            Indexer<UniTuple<Right_>> indexerRight,
            boolean isFiltering) {
        super(shouldExist,
                inputStoreIndexLeftTrackerList, inputStoreIndexRightTrackerList,
                nextNodesTupleLifecycle, isFiltering);
        this.mappingRight = mappingRight;
        this.inputStoreIndexLeftProperties = inputStoreIndexLeftProperties;
        this.inputStoreIndexLeftCounterEntry = inputStoreIndexLeftCounterEntry;
        this.inputStoreIndexRightProperties = inputStoreIndexRightProperties;
        this.inputStoreIndexRightEntry = inputStoreIndexRightEntry;
        this.indexerLeft = indexerLeft;
        this.indexerRight = indexerRight;
    }

    @Override
    public final void insertLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(inputStoreIndexLeftProperties) != null) {
            throw new IllegalStateException("Impossible state: the input for the tuple (" + leftTuple
                    + ") was already added in the tupleStore.");
        }
        IndexProperties indexProperties = createIndexProperties(leftTuple);
        leftTuple.setStore(inputStoreIndexLeftProperties, indexProperties);

        ExistsCounter<LeftTuple_> counter = new ExistsCounter<>(leftTuple);
        ElementAwareListEntry<ExistsCounter<LeftTuple_>> counterEntry = indexerLeft.put(indexProperties, counter);
        updateCounterRight(leftTuple, indexProperties, counter, counterEntry);
        initCounterLeft(counter);
    }

    private void updateCounterRight(LeftTuple_ leftTuple, IndexProperties indexProperties, ExistsCounter<LeftTuple_> counter,
            ElementAwareListEntry<ExistsCounter<LeftTuple_>> counterEntry) {
        leftTuple.setStore(inputStoreIndexLeftCounterEntry, counterEntry);
        if (!isFiltering) {
            counter.countRight = indexerRight.size(indexProperties);
        } else {
            var leftTrackerList = new ElementAwareList<FilteringTracker<LeftTuple_>>();
            indexerRight.forEach(indexProperties,
                    rightTuple -> updateCounterFromLeft(leftTuple, rightTuple, counter, leftTrackerList));
            leftTuple.setStore(inputStoreIndexLeftTrackerList, leftTrackerList);
        }
    }

    @Override
    public final void updateLeft(LeftTuple_ leftTuple) {
        IndexProperties oldIndexProperties = leftTuple.getStore(inputStoreIndexLeftProperties);
        if (oldIndexProperties == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertLeft(leftTuple);
            return;
        }
        IndexProperties newIndexProperties = createIndexProperties(leftTuple);
        ElementAwareListEntry<ExistsCounter<LeftTuple_>> counterEntry = leftTuple.getStore(inputStoreIndexLeftCounterEntry);
        ExistsCounter<LeftTuple_> counter = counterEntry.getElement();

        if (oldIndexProperties.equals(newIndexProperties)) {
            // No need for re-indexing because the index properties didn't change
            // The indexers contain counters in the DEAD state, to track the rightCount.
            if (!isFiltering) {
                updateUnchangedCounterLeft(counter);
            } else {
                // Call filtering for the leftTuple and rightTuple combinations again
                ElementAwareList<FilteringTracker<LeftTuple_>> leftTrackerList =
                        leftTuple.getStore(inputStoreIndexLeftTrackerList);
                leftTrackerList.forEach(FilteringTracker::remove);
                counter.countRight = 0;
                indexerRight.forEach(oldIndexProperties,
                        rightTuple -> updateCounterFromLeft(leftTuple, rightTuple, counter, leftTrackerList));
                updateCounterLeft(counter);
            }
        } else {
            updateIndexerLeft(oldIndexProperties, counterEntry, leftTuple);
            counter.countRight = 0;
            leftTuple.setStore(inputStoreIndexLeftProperties, newIndexProperties);
            updateCounterRight(leftTuple, newIndexProperties, counter, indexerLeft.put(newIndexProperties, counter));
            updateCounterLeft(counter);
        }
    }

    @Override
    public final void retractLeft(LeftTuple_ leftTuple) {
        IndexProperties indexProperties = leftTuple.removeStore(inputStoreIndexLeftProperties);
        if (indexProperties == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ElementAwareListEntry<ExistsCounter<LeftTuple_>> counterEntry = leftTuple.getStore(inputStoreIndexLeftCounterEntry);
        ExistsCounter<LeftTuple_> counter = counterEntry.getElement();
        updateIndexerLeft(indexProperties, counterEntry, leftTuple);
        killCounterLeft(counter);
    }

    private void updateIndexerLeft(IndexProperties indexProperties,
            ElementAwareListEntry<ExistsCounter<LeftTuple_>> counterEntry,
            LeftTuple_ leftTuple) {
        indexerLeft.remove(indexProperties, counterEntry);
        if (isFiltering) {
            ElementAwareList<FilteringTracker<LeftTuple_>> leftTrackerList = leftTuple.getStore(inputStoreIndexLeftTrackerList);
            leftTrackerList.forEach(FilteringTracker::remove);
        }
    }

    @Override
    public final void insertRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightProperties) != null) {
            throw new IllegalStateException("Impossible state: the input for the tuple (" + rightTuple
                    + ") was already added in the tupleStore.");
        }
        IndexProperties indexProperties = mappingRight.apply(rightTuple.factA);
        rightTuple.setStore(inputStoreIndexRightProperties, indexProperties);

        ElementAwareListEntry<UniTuple<Right_>> rightEntry = indexerRight.put(indexProperties, rightTuple);
        rightTuple.setStore(inputStoreIndexRightEntry, rightEntry);
        updateCounterLeft(rightTuple, indexProperties);
    }

    private void updateCounterLeft(UniTuple<Right_> rightTuple, IndexProperties indexProperties) {
        if (!isFiltering) {
            indexerLeft.forEach(indexProperties, this::incrementCounterRight);
        } else {
            var rightTrackerList = new ElementAwareList<FilteringTracker<LeftTuple_>>();
            indexerLeft.forEach(indexProperties, counter -> updateCounterFromRight(rightTuple, counter, rightTrackerList));
            rightTuple.setStore(inputStoreIndexRightTrackerList, rightTrackerList);
        }
    }

    @Override
    public final void updateRight(UniTuple<Right_> rightTuple) {
        IndexProperties oldIndexProperties = rightTuple.getStore(inputStoreIndexRightProperties);
        if (oldIndexProperties == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertRight(rightTuple);
            return;
        }
        IndexProperties newIndexProperties = mappingRight.apply(rightTuple.factA);

        if (oldIndexProperties.equals(newIndexProperties)) {
            // No need for re-indexing because the index properties didn't change
            if (isFiltering) {
                ElementAwareList<FilteringTracker<LeftTuple_>> rightTrackerList = updateRightTrackerList(rightTuple);
                indexerLeft.forEach(oldIndexProperties,
                        counter -> updateCounterFromRight(rightTuple, counter, rightTrackerList));
            }
        } else {
            ElementAwareListEntry<UniTuple<Right_>> rightEntry = rightTuple.getStore(inputStoreIndexRightEntry);
            indexerRight.remove(oldIndexProperties, rightEntry);
            if (!isFiltering) {
                indexerLeft.forEach(oldIndexProperties, this::decrementCounterRight);
            } else {
                updateRightTrackerList(rightTuple);
            }
            rightTuple.setStore(inputStoreIndexRightProperties, newIndexProperties);
            rightEntry = indexerRight.put(newIndexProperties, rightTuple);
            rightTuple.setStore(inputStoreIndexRightEntry, rightEntry);
            updateCounterLeft(rightTuple, newIndexProperties);
        }
    }

    @Override
    public final void retractRight(UniTuple<Right_> rightTuple) {
        IndexProperties indexProperties = rightTuple.removeStore(inputStoreIndexRightProperties);
        if (indexProperties == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ElementAwareListEntry<UniTuple<Right_>> rightEntry = rightTuple.removeStore(inputStoreIndexRightEntry);
        indexerRight.remove(indexProperties, rightEntry);
        if (!isFiltering) {
            indexerLeft.forEach(indexProperties, this::decrementCounterRight);
        } else {
            updateRightTrackerList(rightTuple);
        }
    }

    protected abstract IndexProperties createIndexProperties(LeftTuple_ leftTuple);

}
