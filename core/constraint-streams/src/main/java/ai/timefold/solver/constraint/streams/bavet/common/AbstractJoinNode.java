package ai.timefold.solver.constraint.streams.bavet.common;

import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.ABORTING;
import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.DYING;
import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.UPDATING;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
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
    /**
     * Calls for example {@link AbstractScorer#insert(AbstractTuple)} and/or ...
     */
    private final TupleLifecycle<OutTuple_> nextNodesTupleLifecycle;
    private final boolean isFiltering;
    private final int outputStoreIndexLeftOutEntry;
    private final int outputStoreIndexRightOutEntry;
    protected final DirtyQueue<OutTuple_, OutTuple_> dirtyTupleQueue;

    protected AbstractJoinNode(int inputStoreIndexLeftOutTupleList, int inputStoreIndexRightOutTupleList,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, boolean isFiltering,
            int outputStoreIndexLeftOutEntry, int outputStoreIndexRightOutEntry) {
        this.inputStoreIndexLeftOutTupleList = inputStoreIndexLeftOutTupleList;
        this.inputStoreIndexRightOutTupleList = inputStoreIndexRightOutTupleList;
        this.nextNodesTupleLifecycle = nextNodesTupleLifecycle;
        this.isFiltering = isFiltering;
        this.outputStoreIndexLeftOutEntry = outputStoreIndexLeftOutEntry;
        this.outputStoreIndexRightOutEntry = outputStoreIndexRightOutEntry;
        dirtyTupleQueue = DirtyQueue.ofTuples();
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
        dirtyTupleQueue.insert(outTuple);
    }

    protected final void insertOutTupleFiltered(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
        if (!isFiltering || testFiltering(leftTuple, rightTuple)) {
            insertOutTuple(leftTuple, rightTuple);
        }
    }

    protected final void innerUpdateLeft(LeftTuple_ leftTuple, Consumer<Consumer<UniTuple<Right_>>> rightTupleConsumer) {
        // Prefer an update over retract-insert if possible
        ElementAwareList<OutTuple_> outTupleListLeft = leftTuple.getStore(inputStoreIndexLeftOutTupleList);
        if (!isFiltering) {
            // Propagate the update for downstream filters, matchWeighers, ...
            outTupleListLeft.forEach(outTuple -> updateOutTupleLeft(outTuple, leftTuple));
        } else {
            // Hack: the outTuple has no left/right input tuple reference, use the left/right outList reference instead
            Map<ElementAwareList<OutTuple_>, OutTuple_> rightToOutMap = new IdentityHashMap<>(outTupleListLeft.size());
            outTupleListLeft.forEach(outTuple -> {
                ElementAwareListEntry<OutTuple_> rightOutEntry = outTuple.getStore(outputStoreIndexRightOutEntry);
                rightToOutMap.put(rightOutEntry.getList(), outTuple);

            });
            rightTupleConsumer.accept(rightTuple -> {
                ElementAwareList<OutTuple_> rightOutList = rightTuple.getStore(inputStoreIndexRightOutTupleList);
                processOutTupleUpdate(leftTuple, rightTuple, rightToOutMap, rightOutList);
            });
        }
    }

    private void updateOutTupleLeft(OutTuple_ outTuple, LeftTuple_ leftTuple) {
        setOutTupleLeftFacts(outTuple, leftTuple);
        doUpdateOutTuple(outTuple);
    }

    private void doUpdateOutTuple(OutTuple_ outTuple) {
        switch (outTuple.state) {
            case CREATING:
            case UPDATING:
                // Don't add the tuple to the dirtyTupleQueue twice
                break;
            case OK:
                dirtyTupleQueue.insertWithState(outTuple, UPDATING);
                break;
            // Impossible because they shouldn't linger in the indexes
            case DYING:
            case ABORTING:
            case DEAD:
            default:
                throw new IllegalStateException("Impossible state: The tuple (" + outTuple.state + ") in node (" +
                        this + ") is in an unexpected state (" + outTuple.state + ").");
        }
    }

    protected final void innerUpdateRight(UniTuple<Right_> rightTuple, Consumer<Consumer<LeftTuple_>> leftTupleConsumer) {
        // Prefer an update over retract-insert if possible
        ElementAwareList<OutTuple_> outTupleListRight = rightTuple.getStore(inputStoreIndexRightOutTupleList);
        if (!isFiltering) {
            // Propagate the update for downstream filters, matchWeighers, ...
            outTupleListRight.forEach(outTuple -> {
                setOutTupleRightFact(outTuple, rightTuple);
                doUpdateOutTuple(outTuple);
            });
        } else {
            // Hack: the outTuple has no left/right input tuple reference, use the left/right outList reference instead
            Map<ElementAwareList<OutTuple_>, OutTuple_> leftToOutMap = new IdentityHashMap<>(outTupleListRight.size());
            outTupleListRight.forEach(outTuple -> {
                ElementAwareListEntry<OutTuple_> leftOutEntry = outTuple.getStore(outputStoreIndexLeftOutEntry);
                leftToOutMap.put(leftOutEntry.getList(), outTuple);
            });
            leftTupleConsumer.accept(leftTuple -> {
                ElementAwareList<OutTuple_> leftOutList = leftTuple.getStore(inputStoreIndexLeftOutTupleList);
                processOutTupleUpdate(leftTuple, rightTuple, leftToOutMap, leftOutList);
            });
        }
    }

    private void processOutTupleUpdate(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple,
            Map<ElementAwareList<OutTuple_>, OutTuple_> outMap, ElementAwareList<OutTuple_> outList) {
        OutTuple_ outTuple = outMap.get(outList);
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

    protected final void retractOutTuple(OutTuple_ outTuple) {
        ElementAwareListEntry<OutTuple_> outEntryLeft = outTuple.removeStore(outputStoreIndexLeftOutEntry);
        outEntryLeft.remove();
        ElementAwareListEntry<OutTuple_> outEntryRight = outTuple.removeStore(outputStoreIndexRightOutEntry);
        outEntryRight.remove();
        switch (outTuple.state) {
            case CREATING:
                // Don't add the tuple to the dirtyTupleQueue twice
                // Kill it before it propagates
                dirtyTupleQueue.changeState(outTuple, ABORTING);
                break;
            case OK:
                dirtyTupleQueue.insertWithState(outTuple, DYING);
                break;
            case UPDATING:
                // Don't add the tuple to the dirtyTupleQueue twice
                // Kill the original propagation
                dirtyTupleQueue.changeState(outTuple, DYING);
                break;
            // Impossible because they shouldn't linger in the indexes
            case DYING:
            case ABORTING:
            case DEAD:
            default:
                throw new IllegalStateException("Impossible state: The tuple (" + outTuple.state + ") in node (" +
                        this + ") is in an unexpected state (" + outTuple.state + ").");
        }
    }

    @Override
    public final void calculateScore() {
        dirtyTupleQueue.clear(this, nextNodesTupleLifecycle);
    }

}
