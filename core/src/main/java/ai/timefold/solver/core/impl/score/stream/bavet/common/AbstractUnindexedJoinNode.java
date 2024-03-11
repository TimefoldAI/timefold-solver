package ai.timefold.solver.core.impl.score.stream.bavet.common;

import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.util.ElementAwareList;
import ai.timefold.solver.core.impl.util.ElementAwareListEntry;

/**
 * There is a strong likelihood that any change made to this class
 * should also be made to {@link AbstractIndexedJoinNode}.
 *
 * @param <LeftTuple_>
 * @param <Right_>
 */
public abstract class AbstractUnindexedJoinNode<LeftTuple_ extends AbstractTuple, Right_, OutTuple_ extends AbstractTuple>
        extends AbstractJoinNode<LeftTuple_, Right_, OutTuple_>
        implements LeftTupleLifecycle<LeftTuple_>, RightTupleLifecycle<UniTuple<Right_>> {

    private final int inputStoreIndexLeftEntry;
    private final int inputStoreIndexRightEntry;
    private final ElementAwareList<LeftTuple_> leftTupleList = new ElementAwareList<>();
    private final ElementAwareList<UniTuple<Right_>> rightTupleList = new ElementAwareList<>();

    protected AbstractUnindexedJoinNode(int inputStoreIndexLeftEntry, int inputStoreIndexLeftOutTupleList,
            int inputStoreIndexRightEntry, int inputStoreIndexRightOutTupleList,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, boolean isFiltering, int outputStoreIndexLeftOutEntry,
            int outputStoreIndexRightOutEntry) {
        super(inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, nextNodesTupleLifecycle, isFiltering,
                outputStoreIndexLeftOutEntry, outputStoreIndexRightOutEntry);
        this.inputStoreIndexLeftEntry = inputStoreIndexLeftEntry;
        this.inputStoreIndexRightEntry = inputStoreIndexRightEntry;
    }

    @Override
    public final void insertLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(inputStoreIndexLeftEntry) != null) {
            throw new IllegalStateException("Impossible state: the input for the tuple (" + leftTuple
                    + ") was already added in the tupleStore.");
        }
        ElementAwareListEntry<LeftTuple_> leftEntry = leftTupleList.add(leftTuple);
        leftTuple.setStore(inputStoreIndexLeftEntry, leftEntry);
        ElementAwareList<OutTuple_> outTupleListLeft = new ElementAwareList<>();
        leftTuple.setStore(inputStoreIndexLeftOutTupleList, outTupleListLeft);
        for (UniTuple<Right_> tuple : rightTupleList) {
            insertOutTupleFiltered(leftTuple, tuple);
        }
    }

    @Override
    public final void updateLeft(LeftTuple_ leftTuple) {
        ElementAwareListEntry<LeftTuple_> leftEntry = leftTuple.getStore(inputStoreIndexLeftEntry);
        if (leftEntry == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertLeft(leftTuple);
            return;
        }
        innerUpdateLeft(leftTuple, rightTupleList::forEach);
    }

    @Override
    public final void retractLeft(LeftTuple_ leftTuple) {
        ElementAwareListEntry<LeftTuple_> leftEntry = leftTuple.removeStore(inputStoreIndexLeftEntry);
        if (leftEntry == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ElementAwareList<OutTuple_> outTupleListLeft = leftTuple.removeStore(inputStoreIndexLeftOutTupleList);
        leftEntry.remove();
        outTupleListLeft.forEach(this::retractOutTuple);
    }

    @Override
    public final void insertRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightEntry) != null) {
            throw new IllegalStateException("Impossible state: the input for the tuple (" + rightTuple
                    + ") was already added in the tupleStore.");
        }
        ElementAwareListEntry<UniTuple<Right_>> rightEntry = rightTupleList.add(rightTuple);
        rightTuple.setStore(inputStoreIndexRightEntry, rightEntry);
        ElementAwareList<OutTuple_> outTupleListRight = new ElementAwareList<>();
        rightTuple.setStore(inputStoreIndexRightOutTupleList, outTupleListRight);
        for (LeftTuple_ tuple : leftTupleList) {
            insertOutTupleFiltered(tuple, rightTuple);
        }
    }

    @Override
    public final void updateRight(UniTuple<Right_> rightTuple) {
        ElementAwareListEntry<UniTuple<Right_>> rightEntry = rightTuple.getStore(inputStoreIndexRightEntry);
        if (rightEntry == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertRight(rightTuple);
            return;
        }
        innerUpdateRight(rightTuple, leftTupleList::forEach);
    }

    @Override
    public final void retractRight(UniTuple<Right_> rightTuple) {
        ElementAwareListEntry<UniTuple<Right_>> rightEntry = rightTuple.removeStore(inputStoreIndexRightEntry);
        if (rightEntry == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ElementAwareList<OutTuple_> outTupleListRight = rightTuple.removeStore(inputStoreIndexRightOutTupleList);
        rightEntry.remove();
        outTupleListRight.forEach(this::retractOutTuple);
    }

}
