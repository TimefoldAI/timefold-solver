package ai.timefold.solver.constraint.streams.bavet.common;

import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.ABORTING;
import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.CREATING;
import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.DYING;

import java.util.function.Consumer;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.util.ElementAwareList;
import ai.timefold.solver.core.impl.util.ElementAwareListEntry;

/**
 * This class has two direct children: {@link AbstractIndexedJoinNode} and {@link AbstractUnindexedJoinNode}.
 * The logic in either is identical, except that the latter removes all indexing work.
 * Therefore any time that one of the classes changes,
 * the other should be inspected if it could benefit from applying the change there too.
 *
 * @param <LeftTuple_>
 * @param <Right_>
 */
public abstract class AbstractJoinNode<LeftTuple_ extends AbstractTuple, Right_, OutTuple_ extends AbstractTuple>
        extends AbstractNode
        implements LeftTupleLifecycle<LeftTuple_>, RightTupleLifecycle<UniTuple<Right_>> {

    protected final int inputStoreIndexLeftOutTupleList;
    protected final int inputStoreIndexRightOutTupleList;
    private final boolean isFiltering;
    private final int outputStoreIndexLeftOutEntry;
    private final int outputStoreIndexRightOutEntry;
    private final StaticPropagationQueue<OutTuple_> propagationQueue;

    protected AbstractJoinNode(int inputStoreIndexLeftOutTupleList, int inputStoreIndexRightOutTupleList,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, boolean isFiltering,
            int outputStoreIndexLeftOutEntry, int outputStoreIndexRightOutEntry) {
        this.inputStoreIndexLeftOutTupleList = inputStoreIndexLeftOutTupleList;
        this.inputStoreIndexRightOutTupleList = inputStoreIndexRightOutTupleList;
        this.isFiltering = isFiltering;
        this.outputStoreIndexLeftOutEntry = outputStoreIndexLeftOutEntry;
        this.outputStoreIndexRightOutEntry = outputStoreIndexRightOutEntry;
        this.propagationQueue = new StaticPropagationQueue<>(nextNodesTupleLifecycle);
    }

    protected abstract OutTuple_ createOutTuple(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple);

    protected abstract void setOutTupleLeftFacts(OutTuple_ outTuple, LeftTuple_ leftTuple);

    protected abstract void setOutTupleRightFact(OutTuple_ outTuple, UniTuple<Right_> rightTuple);

    protected abstract boolean testFiltering(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple);

    protected final void insertOutTuple(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
        OutTuple_ outTuple = createOutTuple(leftTuple, rightTuple);
        ElementAwareList<OutTuple_> outTupleListLeft = leftTuple.getStore(inputStoreIndexLeftOutTupleList);
        ElementAwareListEntry<OutTuple_> outEntryLeft = outTupleListLeft.add(outTuple);
        outTuple.setStore(outputStoreIndexLeftOutEntry, outEntryLeft);
        ElementAwareList<OutTuple_> outTupleListRight = rightTuple.getStore(inputStoreIndexRightOutTupleList);
        ElementAwareListEntry<OutTuple_> outEntryRight = outTupleListRight.add(outTuple);
        outTuple.setStore(outputStoreIndexRightOutEntry, outEntryRight);
        propagationQueue.insert(outTuple);
    }

    protected final void insertOutTupleFiltered(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
        if (!isFiltering || testFiltering(leftTuple, rightTuple)) {
            insertOutTuple(leftTuple, rightTuple);
        }
    }

    protected final void innerUpdateLeft(LeftTuple_ leftTuple, Consumer<Consumer<UniTuple<Right_>>> rightTupleConsumer) {
        // Prefer an update over retract-insert if possible
        ElementAwareList<OutTuple_> outTupleListLeft = leftTuple.getStore(inputStoreIndexLeftOutTupleList);
        // Propagate the update for downstream filters, matchWeighers, ...
        if (!isFiltering) {
            for (OutTuple_ outTuple : outTupleListLeft) {
                updateOutTupleLeft(outTuple, leftTuple);
            }
        } else {
            rightTupleConsumer.accept(rightTuple -> {
                ElementAwareList<OutTuple_> rightOutList = rightTuple.getStore(inputStoreIndexRightOutTupleList);
                processOutTupleUpdate(leftTuple, rightTuple, rightOutList, outTupleListLeft, outputStoreIndexRightOutEntry);
            });
        }
    }

    private void updateOutTupleLeft(OutTuple_ outTuple, LeftTuple_ leftTuple) {
        setOutTupleLeftFacts(outTuple, leftTuple);
        doUpdateOutTuple(outTuple);
    }

    private void doUpdateOutTuple(OutTuple_ outTuple) {
        TupleState state = outTuple.state;
        if (!state.isActive()) { // Impossible because they shouldn't linger in the indexes.
            throw new IllegalStateException("Impossible state: The tuple (" + outTuple.state + ") in node (" +
                    this + ") is in an unexpected state (" + outTuple.state + ").");
        } else if (state != TupleState.OK) { // Already in the queue in the correct state.
            return;
        }
        propagationQueue.update(outTuple);
    }

    protected final void innerUpdateRight(UniTuple<Right_> rightTuple, Consumer<Consumer<LeftTuple_>> leftTupleConsumer) {
        // Prefer an update over retract-insert if possible
        ElementAwareList<OutTuple_> outTupleListRight = rightTuple.getStore(inputStoreIndexRightOutTupleList);
        if (!isFiltering) {
            // Propagate the update for downstream filters, matchWeighers, ...
            for (OutTuple_ outTuple : outTupleListRight) {
                setOutTupleRightFact(outTuple, rightTuple);
                doUpdateOutTuple(outTuple);
            }
        } else {
            leftTupleConsumer.accept(leftTuple -> {
                ElementAwareList<OutTuple_> leftOutList = leftTuple.getStore(inputStoreIndexLeftOutTupleList);
                processOutTupleUpdate(leftTuple, rightTuple, leftOutList, outTupleListRight, outputStoreIndexLeftOutEntry);
            });
        }
    }

    private void processOutTupleUpdate(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple, ElementAwareList<OutTuple_> outList,
            ElementAwareList<OutTuple_> outTupleList, int outputStoreIndexOutEntry) {
        OutTuple_ outTuple = findOutTuple(outTupleList, outList, outputStoreIndexOutEntry);
        if (testFiltering(leftTuple, rightTuple)) {
            if (outTuple == null) {
                insertOutTuple(leftTuple, rightTuple);
            } else {
                updateOutTupleLeft(outTuple, leftTuple);
            }
        } else {
            if (outTuple != null) {
                retractOutTuple(outTuple);
            }
        }
    }

    private OutTuple_ findOutTuple(ElementAwareList<OutTuple_> outTupleList, ElementAwareList<OutTuple_> outList,
            int outputStoreIndexOutEntry) {
        // Hack: the outTuple has no left/right input tuple reference, use the left/right outList reference instead.
        for (OutTuple_ outTuple : outTupleList) {
            ElementAwareListEntry<OutTuple_> outEntry = outTuple.getStore(outputStoreIndexOutEntry);
            ElementAwareList<OutTuple_> outEntryList = outEntry.getList();
            if (outList == outEntryList) {
                return outTuple;
            }
        }
        return null;
    }

    protected final void retractOutTuple(OutTuple_ outTuple) {
        ElementAwareListEntry<OutTuple_> outEntryLeft = outTuple.removeStore(outputStoreIndexLeftOutEntry);
        outEntryLeft.remove();
        ElementAwareListEntry<OutTuple_> outEntryRight = outTuple.removeStore(outputStoreIndexRightOutEntry);
        outEntryRight.remove();
        TupleState state = outTuple.state;
        if (!state.isActive()) {
            // Impossible because they shouldn't linger in the indexes.
            throw new IllegalStateException("Impossible state: The tuple (" + outTuple.state + ") in node (" + this
                    + ") is in an unexpected state (" + outTuple.state + ").");
        }
        propagationQueue.retract(outTuple, state == CREATING ? ABORTING : DYING);
    }

    @Override
    public final void calculateScore() {
        propagationQueue.propagateAndClear();
    }

}
