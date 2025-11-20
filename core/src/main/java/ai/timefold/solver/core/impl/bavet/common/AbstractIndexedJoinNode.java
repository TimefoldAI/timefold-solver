package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.index.IndexedSet;
import ai.timefold.solver.core.impl.bavet.common.index.Indexer;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory.KeysExtractor;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory.UniKeysExtractor;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.OutputStoreSizeTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

/**
 * There is a strong likelihood that any change to this class, which is not related to indexing,
 * should also be made to {@link AbstractUnindexedJoinNode}.
 *
 * @param <LeftTuple_>
 * @param <Right_>
 */
public abstract class AbstractIndexedJoinNode<LeftTuple_ extends AbstractTuple, Right_, OutTuple_ extends AbstractTuple>
        extends AbstractJoinNode<LeftTuple_, Right_, OutTuple_>
        implements LeftTupleLifecycle<LeftTuple_>, RightTupleLifecycle<UniTuple<Right_>> {

    private final KeysExtractor<LeftTuple_> keysExtractorLeft;
    private final UniKeysExtractor<Right_> keysExtractorRight;
    private final int inputStoreIndexLeftKeys;
    private final int inputStoreIndexRightKeys;
    private final int outputStoreIndexLeftPosition;
    private final int outputStoreIndexRightPosition;

    /**
     * Calls for example {@link AbstractScorer#insert(AbstractTuple)} and/or ...
     */
    private final Indexer<LeftTuple_> indexerLeft;
    private final Indexer<UniTuple<Right_>> indexerRight;

    protected AbstractIndexedJoinNode(KeysExtractor<LeftTuple_> keysExtractorLeft, IndexerFactory<Right_> indexerFactory,
            TupleStorePositionTracker leftTupleStorePositionTracker, TupleStorePositionTracker rightTupleStorePositionTracker,
            OutputStoreSizeTracker outputStoreSizeTracker, TupleLifecycle<OutTuple_> nextNodesTupleLifecycle,
            boolean isFiltering) {
        super(leftTupleStorePositionTracker, rightTupleStorePositionTracker, outputStoreSizeTracker, nextNodesTupleLifecycle,
                isFiltering);
        this.keysExtractorLeft = keysExtractorLeft;
        this.keysExtractorRight = indexerFactory.buildRightKeysExtractor();
        this.inputStoreIndexLeftKeys = leftTupleStorePositionTracker.reserveNextAvailablePosition();
        this.inputStoreIndexRightKeys = rightTupleStorePositionTracker.reserveNextAvailablePosition();
        this.outputStoreIndexLeftPosition = outputStoreSizeTracker.reserveNextAvailablePosition();
        this.outputStoreIndexRightPosition = outputStoreSizeTracker.reserveNextAvailablePosition();
        this.indexerLeft = indexerFactory.buildIndexer(true,
                new TuplePositionTracker<>(leftTupleStorePositionTracker.reserveNextAvailablePosition()));
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
        var outTupleSetLeft = new IndexedSet<>(new TuplePositionTracker<>(outputStoreIndexLeftPosition));
        leftTuple.setStore(inputStoreIndexLeftOutTupleSet, outTupleSetLeft);
        indexAndPropagateLeft(leftTuple, indexKeys);
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
        if (oldIndexKeys.equals(newIndexKeys)) {
            // No need for re-indexing because the index keys didn't change
            // Prefer an update over retract-insert if possible
            innerUpdateLeft(leftTuple, consumer -> indexerRight.forEach(oldIndexKeys, consumer));
        } else {
            indexerLeft.remove(oldIndexKeys, leftTuple);
            IndexedSet<OutTuple_> outTupleSetLeft = leftTuple.getStore(inputStoreIndexLeftOutTupleSet);
            outTupleSetLeft.forEach(this::retractOutTuple);
            // outTupleSetLeft is now empty, no need for leftTuple.setStore(...);
            indexAndPropagateLeft(leftTuple, newIndexKeys);
        }
    }

    private void indexAndPropagateLeft(LeftTuple_ leftTuple, Object indexKeys) {
        leftTuple.setStore(inputStoreIndexLeftKeys, indexKeys);
        indexerLeft.put(indexKeys, leftTuple);
        indexerRight.forEach(indexKeys, rightTuple -> insertOutTupleFiltered(leftTuple, rightTuple));
    }

    @Override
    public final void retractLeft(LeftTuple_ leftTuple) {
        var indexKeys = leftTuple.removeStore(inputStoreIndexLeftKeys);
        if (indexKeys == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        IndexedSet<OutTuple_> outTupleSetLeft = leftTuple.removeStore(inputStoreIndexLeftOutTupleSet);
        indexerLeft.remove(indexKeys, leftTuple);
        outTupleSetLeft.forEach(this::retractOutTuple);
    }

    @Override
    public final void insertRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightKeys) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(rightTuple));
        }
        var indexKeys = keysExtractorRight.apply(rightTuple);
        var outTupleSetRight = new IndexedSet<>(new TuplePositionTracker<>(outputStoreIndexRightPosition));
        rightTuple.setStore(inputStoreIndexRightOutTupleSet, outTupleSetRight);
        indexAndPropagateRight(rightTuple, indexKeys);
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
            // Prefer an update over retract-insert if possible
            innerUpdateRight(rightTuple, consumer -> indexerLeft.forEach(oldIndexKeys, consumer));
        } else {
            IndexedSet<OutTuple_> outTupleSetRight = rightTuple.getStore(inputStoreIndexRightOutTupleSet);
            indexerRight.remove(oldIndexKeys, rightTuple);
            outTupleSetRight.forEach(this::retractOutTuple);
            // outTupleSetRight is now empty, no need for rightTuple.setStore(...);
            indexAndPropagateRight(rightTuple, newIndexKeys);
        }
    }

    private void indexAndPropagateRight(UniTuple<Right_> rightTuple, Object indexKeys) {
        rightTuple.setStore(inputStoreIndexRightKeys, indexKeys);
        indexerRight.put(indexKeys, rightTuple);
        indexerLeft.forEach(indexKeys, leftTuple -> insertOutTupleFiltered(leftTuple, rightTuple));
    }

    @Override
    public final void retractRight(UniTuple<Right_> rightTuple) {
        var indexKeys = rightTuple.removeStore(inputStoreIndexRightKeys);
        if (indexKeys == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        IndexedSet<OutTuple_> outTupleSetRight = rightTuple.removeStore(inputStoreIndexRightOutTupleSet);
        indexerRight.remove(indexKeys, rightTuple);
        outTupleSetRight.forEach(this::retractOutTuple);
    }

}
