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
    private final int inputStoreIndexLeftEntry;
    private final int inputStoreIndexRightKeys;
    private final int inputStoreIndexRightEntry;
    /**
     * Calls for example {@link AbstractScorer#insert(AbstractTuple)} and/or ...
     */
    private final Indexer<LeftTuple_> indexerLeft;
    private final Indexer<UniTuple<Right_>> indexerRight;

    protected AbstractIndexedJoinNode(KeysExtractor<LeftTuple_> keysExtractorLeft, IndexerFactory<Right_> indexerFactory,
            int inputStoreIndexLeftKeys, int inputStoreIndexLeftEntry, int inputStoreIndexLeftOutTupleList,
            int inputStoreIndexRightKeys, int inputStoreIndexRightEntry, int inputStoreIndexRightOutTupleList,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, boolean isFiltering, int outputStoreIndexLeftOutEntry,
            int outputStoreIndexRightOutEntry) {
        super(inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, nextNodesTupleLifecycle, isFiltering,
                outputStoreIndexLeftOutEntry, outputStoreIndexRightOutEntry);
        this.keysExtractorLeft = keysExtractorLeft;
        this.keysExtractorRight = indexerFactory.buildRightKeysExtractor();
        this.inputStoreIndexLeftKeys = inputStoreIndexLeftKeys;
        this.inputStoreIndexLeftEntry = inputStoreIndexLeftEntry;
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
        var outTupleListLeft = new ElementAwareList<OutTuple_>();
        leftTuple.setStore(inputStoreIndexLeftOutTupleList, outTupleListLeft);
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
            ElementAwareListEntry<LeftTuple_> leftEntry = leftTuple.getStore(inputStoreIndexLeftEntry);
            ElementAwareList<OutTuple_> outTupleListLeft = leftTuple.getStore(inputStoreIndexLeftOutTupleList);
            indexerLeft.remove(oldIndexKeys, leftEntry);
            outTupleListLeft.forEach(this::retractOutTuple);
            // outTupleListLeft is now empty
            // No need for leftTuple.setStore(inputStoreIndexLeftOutTupleList, outTupleListLeft);
            indexAndPropagateLeft(leftTuple, newIndexKeys);
        }
    }

    private void indexAndPropagateLeft(LeftTuple_ leftTuple, Object indexKeys) {
        leftTuple.setStore(inputStoreIndexLeftKeys, indexKeys);
        var leftEntry = indexerLeft.put(indexKeys, leftTuple);
        leftTuple.setStore(inputStoreIndexLeftEntry, leftEntry);
        indexerRight.forEach(indexKeys, rightTuple -> insertOutTupleFiltered(leftTuple, rightTuple));
    }

    @Override
    public final void retractLeft(LeftTuple_ leftTuple) {
        var indexKeys = leftTuple.removeStore(inputStoreIndexLeftKeys);
        if (indexKeys == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ElementAwareListEntry<LeftTuple_> leftEntry = leftTuple.removeStore(inputStoreIndexLeftEntry);
        ElementAwareList<OutTuple_> outTupleListLeft = leftTuple.removeStore(inputStoreIndexLeftOutTupleList);
        indexerLeft.remove(indexKeys, leftEntry);
        outTupleListLeft.forEach(this::retractOutTuple);
    }

    @Override
    public final void insertRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightKeys) != null) {
            throw new IllegalStateException("Impossible state: the input for the tuple (" + rightTuple
                    + ") was already added in the tupleStore.");
        }
        var indexKeys = keysExtractorRight.apply(rightTuple);
        var outTupleListRight = new ElementAwareList<OutTuple_>();
        rightTuple.setStore(inputStoreIndexRightOutTupleList, outTupleListRight);
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
            ElementAwareListEntry<UniTuple<Right_>> rightEntry = rightTuple.getStore(inputStoreIndexRightEntry);
            ElementAwareList<OutTuple_> outTupleListRight = rightTuple.getStore(inputStoreIndexRightOutTupleList);
            indexerRight.remove(oldIndexKeys, rightEntry);
            outTupleListRight.forEach(this::retractOutTuple);
            // outTupleListRight is now empty
            // No need for rightTuple.setStore(inputStoreIndexRightOutTupleList, outTupleListRight);
            indexAndPropagateRight(rightTuple, newIndexKeys);
        }
    }

    private void indexAndPropagateRight(UniTuple<Right_> rightTuple, Object indexKeys) {
        rightTuple.setStore(inputStoreIndexRightKeys, indexKeys);
        var rightEntry = indexerRight.put(indexKeys, rightTuple);
        rightTuple.setStore(inputStoreIndexRightEntry, rightEntry);
        indexerLeft.forEach(indexKeys, leftTuple -> insertOutTupleFiltered(leftTuple, rightTuple));
    }

    @Override
    public final void retractRight(UniTuple<Right_> rightTuple) {
        var indexKeys = rightTuple.removeStore(inputStoreIndexRightKeys);
        if (indexKeys == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ElementAwareListEntry<UniTuple<Right_>> rightEntry = rightTuple.removeStore(inputStoreIndexRightEntry);
        ElementAwareList<OutTuple_> outTupleListRight = rightTuple.removeStore(inputStoreIndexRightOutTupleList);
        indexerRight.remove(indexKeys, rightEntry);
        outTupleListRight.forEach(this::retractOutTuple);
    }

}
