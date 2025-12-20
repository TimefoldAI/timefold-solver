package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.index.Indexer;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory.KeysExtractor;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory.UniKeysExtractor;
import ai.timefold.solver.core.impl.bavet.common.tuple.InOutTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.util.ElementAwareLinkedList;

/**
 * There is a strong likelihood that any change to this class, which is not related to indexing,
 * should also be made to {@link AbstractUnindexedJoinNode}.
 *
 * @param <LeftTuple_>
 * @param <Right_>
 */
public abstract class AbstractIndexedJoinNode<LeftTuple_ extends Tuple, Right_, OutTuple_ extends Tuple>
        extends AbstractJoinNode<LeftTuple_, Right_, OutTuple_>
        implements LeftTupleLifecycle<LeftTuple_>, RightTupleLifecycle<UniTuple<Right_>> {

    private final KeysExtractor<LeftTuple_> keysExtractorLeft;
    private final UniKeysExtractor<Right_> keysExtractorRight;
    private final int inputStoreIndexLeftCompositeKey;
    private final int inputStoreIndexLeftEntry;
    private final int inputStoreIndexRightCompositeKey;
    private final int inputStoreIndexRightEntry;
    /**
     * Calls for example {@link AbstractScorer#insert(Tuple)} and/or ...
     */
    private final Indexer<LeftTuple_> indexerLeft;
    private final Indexer<UniTuple<Right_>> indexerRight;

    protected AbstractIndexedJoinNode(KeysExtractor<LeftTuple_> keysExtractorLeft, IndexerFactory<Right_> indexerFactory,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, boolean isFiltering,
            InOutTupleStorePositionTracker tupleStorePositionTracker) {
        super(nextNodesTupleLifecycle, isFiltering, tupleStorePositionTracker);
        this.keysExtractorLeft = keysExtractorLeft;
        this.keysExtractorRight = indexerFactory.buildRightKeysExtractor();
        this.inputStoreIndexLeftCompositeKey = tupleStorePositionTracker.reserveNextLeft();
        this.inputStoreIndexLeftEntry = tupleStorePositionTracker.reserveNextLeft();
        this.inputStoreIndexRightCompositeKey = tupleStorePositionTracker.reserveNextRight();
        this.inputStoreIndexRightEntry = tupleStorePositionTracker.reserveNextRight();
        this.indexerLeft = indexerFactory.buildIndexer(true);
        this.indexerRight = indexerFactory.buildIndexer(false);
    }

    @Override
    public final void insertLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(inputStoreIndexLeftCompositeKey) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(leftTuple));
        }
        var compositeKey = keysExtractorLeft.apply(leftTuple);
        leftTuple.setStore(inputStoreIndexLeftOutTupleList, new ElementAwareLinkedList<OutTuple_>());
        indexAndPropagateLeft(leftTuple, compositeKey);
    }

    @Override
    public final void updateLeft(LeftTuple_ leftTuple) {
        var oldCompositeKey = leftTuple.getStore(inputStoreIndexLeftCompositeKey);
        if (oldCompositeKey == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertLeft(leftTuple);
            return;
        }
        var newCompositeKey = keysExtractorLeft.apply(leftTuple);
        if (oldCompositeKey.equals(newCompositeKey)) {
            // No need for re-indexing because the index keys didn't change
            // Prefer an update over retract-insert if possible
            innerUpdateLeft(leftTuple, consumer -> indexerRight.forEach(oldCompositeKey, consumer));
        } else {
            ElementAwareLinkedList<OutTuple_> outTupleListLeft = leftTuple.getStore(inputStoreIndexLeftOutTupleList);
            indexerLeft.remove(oldCompositeKey, leftTuple.getStore(inputStoreIndexLeftEntry));
            outTupleListLeft.clear(this::retractOutTupleByLeft);
            // outTupleListLeft is now empty
            // No need for leftTuple.setStore(inputStoreIndexLeftOutTupleList, outTupleListLeft);
            indexAndPropagateLeft(leftTuple, newCompositeKey);
        }
    }

    private void indexAndPropagateLeft(LeftTuple_ leftTuple, Object compositeKey) {
        leftTuple.setStore(inputStoreIndexLeftCompositeKey, compositeKey);
        leftTuple.setStore(inputStoreIndexLeftEntry, indexerLeft.put(compositeKey, leftTuple));
        if (!leftTuple.getState().isActive()) {
            // Assume the following scenario:
            // - The join is of two entities of the same type, both filtering out unassigned.
            // - One entity became unassigned, so the outTuple is getting retracted.
            // - The other entity became assigned, and is therefore getting inserted.
            //
            // This means the filter would be called with (unassignedEntity, assignedEntity),
            // which breaks the expectation that the filter is only called on two assigned entities
            // and requires adding null checks to the filter for something that should intuitively be impossible.
            // We avoid this situation as it is clear that it is pointless to insert this tuple.
            //
            // It is possible that the same problem would exist coming from the other side as well,
            // and therefore the right tuple would have to be checked for active state as well.
            // However, no such issue could have been reproduced; when in doubt, leave it out.
            return;
        }
        indexerRight.forEach(compositeKey, rightTuple -> insertOutTupleFiltered(leftTuple, rightTuple));
    }

    @Override
    public final void retractLeft(LeftTuple_ leftTuple) {
        var compositeKey = leftTuple.removeStore(inputStoreIndexLeftCompositeKey);
        if (compositeKey == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ElementAwareLinkedList<OutTuple_> outTupleListLeft = leftTuple.removeStore(inputStoreIndexLeftOutTupleList);
        indexerLeft.remove(compositeKey, leftTuple.removeStore(inputStoreIndexLeftEntry));
        outTupleListLeft.clear(this::retractOutTupleByLeft);
    }

    @Override
    public final void insertRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightCompositeKey) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(rightTuple));
        }
        var compositeKey = keysExtractorRight.apply(rightTuple);
        rightTuple.setStore(inputStoreIndexRightOutTupleList, new ElementAwareLinkedList<OutTuple_>());
        indexAndPropagateRight(rightTuple, compositeKey);
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
            // Prefer an update over retract-insert if possible
            innerUpdateRight(rightTuple, consumer -> indexerLeft.forEach(oldCompositeKey, consumer));
        } else {
            ElementAwareLinkedList<OutTuple_> outTupleListRight = rightTuple.getStore(inputStoreIndexRightOutTupleList);
            indexerRight.remove(oldCompositeKey, rightTuple.getStore(inputStoreIndexRightEntry));
            outTupleListRight.clear(this::retractOutTupleByRight);
            // outTupleListRight is now empty
            // No need for rightTuple.setStore(inputStoreIndexRightOutTupleList, outTupleListRight);
            indexAndPropagateRight(rightTuple, newCompositeKey);
        }
    }

    private void indexAndPropagateRight(UniTuple<Right_> rightTuple, Object compositeKey) {
        rightTuple.setStore(inputStoreIndexRightCompositeKey, compositeKey);
        rightTuple.setStore(inputStoreIndexRightEntry, indexerRight.put(compositeKey, rightTuple));
        indexerLeft.forEach(compositeKey, leftTuple -> insertOutTupleFilteredFromLeft(leftTuple, rightTuple));
    }

    @Override
    public final void retractRight(UniTuple<Right_> rightTuple) {
        var compositeKey = rightTuple.removeStore(inputStoreIndexRightCompositeKey);
        if (compositeKey == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ElementAwareLinkedList<OutTuple_> outTupleListRight = rightTuple.removeStore(inputStoreIndexRightOutTupleList);
        indexerRight.remove(compositeKey, rightTuple.removeStore(inputStoreIndexRightEntry));
        outTupleListRight.clear(this::retractOutTupleByRight);
    }

}
