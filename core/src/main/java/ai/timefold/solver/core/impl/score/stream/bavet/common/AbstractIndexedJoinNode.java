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
 * should also be made to {@link AbstractUnindexedJoinNode}.
 *
 * @param <LeftTuple_>
 * @param <Right_>
 */
public abstract class AbstractIndexedJoinNode<LeftTuple_ extends AbstractTuple, Right_, OutTuple_ extends AbstractTuple>
        extends AbstractJoinNode<LeftTuple_, Right_, OutTuple_>
        implements LeftTupleLifecycle<LeftTuple_>, RightTupleLifecycle<UniTuple<Right_>> {

    private final Function<Right_, IndexProperties> mappingRight;
    private final int inputStoreIndexLeftProperties;
    private final int inputStoreIndexLeftEntry;
    private final int inputStoreIndexRightProperties;
    private final int inputStoreIndexRightEntry;
    /**
     * Calls for example {@link AbstractScorer#insert(AbstractTuple)} and/or ...
     */
    private final Indexer<LeftTuple_> indexerLeft;
    private final Indexer<UniTuple<Right_>> indexerRight;

    protected AbstractIndexedJoinNode(Function<Right_, IndexProperties> mappingRight, int inputStoreIndexLeftProperties,
            int inputStoreIndexLeftEntry, int inputStoreIndexLeftOutTupleList, int inputStoreIndexRightProperties,
            int inputStoreIndexRightEntry, int inputStoreIndexRightOutTupleList,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, boolean isFiltering, int outputStoreIndexLeftOutEntry,
            int outputStoreIndexRightOutEntry, Indexer<LeftTuple_> indexerLeft, Indexer<UniTuple<Right_>> indexerRight) {
        super(inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, nextNodesTupleLifecycle, isFiltering,
                outputStoreIndexLeftOutEntry, outputStoreIndexRightOutEntry);
        this.mappingRight = mappingRight;
        this.inputStoreIndexLeftProperties = inputStoreIndexLeftProperties;
        this.inputStoreIndexLeftEntry = inputStoreIndexLeftEntry;
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
        IndexProperties indexProperties = createIndexPropertiesLeft(leftTuple);

        ElementAwareList<OutTuple_> outTupleListLeft = new ElementAwareList<>();
        leftTuple.setStore(inputStoreIndexLeftOutTupleList, outTupleListLeft);
        indexAndPropagateLeft(leftTuple, indexProperties);
    }

    @Override
    public final void updateLeft(LeftTuple_ leftTuple) {
        IndexProperties oldIndexProperties = leftTuple.getStore(inputStoreIndexLeftProperties);
        if (oldIndexProperties == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertLeft(leftTuple);
            return;
        }
        IndexProperties newIndexProperties = createIndexPropertiesLeft(leftTuple);
        if (oldIndexProperties.equals(newIndexProperties)) {
            // No need for re-indexing because the index properties didn't change
            // Prefer an update over retract-insert if possible
            innerUpdateLeft(leftTuple, consumer -> indexerRight.forEach(oldIndexProperties, consumer));
        } else {
            ElementAwareListEntry<LeftTuple_> leftEntry = leftTuple.getStore(inputStoreIndexLeftEntry);
            ElementAwareList<OutTuple_> outTupleListLeft = leftTuple.getStore(inputStoreIndexLeftOutTupleList);
            indexerLeft.remove(oldIndexProperties, leftEntry);
            outTupleListLeft.forEach(this::retractOutTuple);
            // outTupleListLeft is now empty
            // No need for leftTuple.setStore(inputStoreIndexLeftOutTupleList, outTupleListLeft);
            indexAndPropagateLeft(leftTuple, newIndexProperties);
        }
    }

    private void indexAndPropagateLeft(LeftTuple_ leftTuple, IndexProperties indexProperties) {
        leftTuple.setStore(inputStoreIndexLeftProperties, indexProperties);
        ElementAwareListEntry<LeftTuple_> leftEntry = indexerLeft.put(indexProperties, leftTuple);
        leftTuple.setStore(inputStoreIndexLeftEntry, leftEntry);
        indexerRight.forEach(indexProperties, rightTuple -> insertOutTupleFiltered(leftTuple, rightTuple));
    }

    @Override
    public final void retractLeft(LeftTuple_ leftTuple) {
        IndexProperties indexProperties = leftTuple.removeStore(inputStoreIndexLeftProperties);
        if (indexProperties == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ElementAwareListEntry<LeftTuple_> leftEntry = leftTuple.removeStore(inputStoreIndexLeftEntry);
        ElementAwareList<OutTuple_> outTupleListLeft = leftTuple.removeStore(inputStoreIndexLeftOutTupleList);
        indexerLeft.remove(indexProperties, leftEntry);
        outTupleListLeft.forEach(this::retractOutTuple);
    }

    @Override
    public final void insertRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightProperties) != null) {
            throw new IllegalStateException("Impossible state: the input for the tuple (" + rightTuple
                    + ") was already added in the tupleStore.");
        }
        IndexProperties indexProperties = mappingRight.apply(rightTuple.factA);

        ElementAwareList<OutTuple_> outTupleListRight = new ElementAwareList<>();
        rightTuple.setStore(inputStoreIndexRightOutTupleList, outTupleListRight);
        indexAndPropagateRight(rightTuple, indexProperties);
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
            // Prefer an update over retract-insert if possible
            innerUpdateRight(rightTuple, consumer -> indexerLeft.forEach(oldIndexProperties, consumer));
        } else {
            ElementAwareListEntry<UniTuple<Right_>> rightEntry = rightTuple.getStore(inputStoreIndexRightEntry);
            ElementAwareList<OutTuple_> outTupleListRight = rightTuple.getStore(inputStoreIndexRightOutTupleList);
            indexerRight.remove(oldIndexProperties, rightEntry);
            outTupleListRight.forEach(this::retractOutTuple);
            // outTupleListRight is now empty
            // No need for rightTuple.setStore(inputStoreIndexRightOutTupleList, outTupleListRight);
            indexAndPropagateRight(rightTuple, newIndexProperties);
        }
    }

    private void indexAndPropagateRight(UniTuple<Right_> rightTuple, IndexProperties indexProperties) {
        rightTuple.setStore(inputStoreIndexRightProperties, indexProperties);
        ElementAwareListEntry<UniTuple<Right_>> rightEntry = indexerRight.put(indexProperties, rightTuple);
        rightTuple.setStore(inputStoreIndexRightEntry, rightEntry);
        indexerLeft.forEach(indexProperties, leftTuple -> insertOutTupleFiltered(leftTuple, rightTuple));
    }

    @Override
    public final void retractRight(UniTuple<Right_> rightTuple) {
        IndexProperties indexProperties = rightTuple.removeStore(inputStoreIndexRightProperties);
        if (indexProperties == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ElementAwareListEntry<UniTuple<Right_>> rightEntry = rightTuple.removeStore(inputStoreIndexRightEntry);
        ElementAwareList<OutTuple_> outTupleListRight = rightTuple.removeStore(inputStoreIndexRightOutTupleList);
        indexerRight.remove(indexProperties, rightEntry);
        outTupleListRight.forEach(this::retractOutTuple);
    }

    protected abstract IndexProperties createIndexPropertiesLeft(LeftTuple_ leftTuple);

}
