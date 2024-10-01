package ai.timefold.solver.core.impl.score.stream.bavet.common;

import java.util.function.BiConsumer;

import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;
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
    private final int outputStoreIndexLeftOutEntry;
    private final int outputStoreIndexRightOutEntry;

    protected final TupleConsumer<LeftTuple_, Right_> leftTupleUpdater;
    protected final TupleConsumer<LeftTuple_, Right_> rightTupleUpdater;
    protected final TupleConsumer<LeftTuple_, Right_> outTupleInserter;
    private final StaticPropagationQueue<OutTuple_> propagationQueue;

    protected AbstractJoinNode(int inputStoreIndexLeftOutTupleList, int inputStoreIndexRightOutTupleList,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, boolean isFiltering,
            int outputStoreIndexLeftOutEntry, int outputStoreIndexRightOutEntry) {
        this.inputStoreIndexLeftOutTupleList = inputStoreIndexLeftOutTupleList;
        this.inputStoreIndexRightOutTupleList = inputStoreIndexRightOutTupleList;
        this.outputStoreIndexLeftOutEntry = outputStoreIndexLeftOutEntry;
        this.outputStoreIndexRightOutEntry = outputStoreIndexRightOutEntry;

        this.leftTupleUpdater = isFiltering ? this::updateLeftTupleWithFiltering : this::updateLeftTupleWithoutFiltering;
        this.rightTupleUpdater = isFiltering ? this::updateRightTupleWithFiltering : this::updateRightTupleWithoutFiltering;
        this.outTupleInserter = isFiltering ? this::insertOutTupleWithFiltering : this::insertOutTupleWithoutFiltering;
        this.propagationQueue = new StaticPropagationQueue<>(nextNodesTupleLifecycle);
    }

    protected abstract OutTuple_ createOutTuple(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple);

    protected abstract void setOutTupleLeftFacts(OutTuple_ outTuple, LeftTuple_ leftTuple);

    protected abstract void setOutTupleRightFact(OutTuple_ outTuple, UniTuple<Right_> rightTuple);

    protected abstract boolean testFiltering(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple);

    private void insertOutTupleWithFiltering(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
        if (testFiltering(leftTuple, rightTuple)) {
            insertOutTupleWithoutFiltering(leftTuple, rightTuple);
        }
    }

    private void insertOutTupleWithoutFiltering(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
        var outTuple = createOutTuple(leftTuple, rightTuple);
        ElementAwareList<OutTuple_> outTupleListLeft = leftTuple.getStore(inputStoreIndexLeftOutTupleList);
        var outEntryLeft = outTupleListLeft.add(outTuple);
        outTuple.setStore(outputStoreIndexLeftOutEntry, outEntryLeft);
        ElementAwareList<OutTuple_> outTupleListRight = rightTuple.getStore(inputStoreIndexRightOutTupleList);
        var outEntryRight = outTupleListRight.add(outTuple);
        outTuple.setStore(outputStoreIndexRightOutEntry, outEntryRight);
        propagationQueue.insert(outTuple);
    }

    private void updateOutTupleLeft(OutTuple_ outTuple, LeftTuple_ leftTuple) {
        setOutTupleLeftFacts(outTuple, leftTuple);
        doUpdateOutTuple(outTuple);
    }

    private void doUpdateOutTuple(OutTuple_ outTuple) {
        var state = outTuple.state;
        if (!state.isActive()) { // Impossible because they shouldn't linger in the indexes.
            throw new IllegalStateException("Impossible state: The tuple (" + outTuple.state + ") in node (" +
                    this + ") is in an unexpected state (" + outTuple.state + ").");
        } else if (state != TupleState.OK) { // Already in the queue in the correct state.
            return;
        }
        propagationQueue.update(outTuple);
    }

    private void updateLeftTupleWithoutFiltering(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
        ElementAwareList<OutTuple_> outTupleListLeft = leftTuple.getStore(inputStoreIndexLeftOutTupleList);
        for (var outTuple : outTupleListLeft) {
            updateOutTupleLeft(outTuple, leftTuple);
        }
    }

    private void updateRightTupleWithoutFiltering(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
        ElementAwareList<OutTuple_> outTupleListRight = rightTuple.getStore(inputStoreIndexRightOutTupleList);
        for (var outTuple : outTupleListRight) {
            setOutTupleRightFact(outTuple, rightTuple);
            doUpdateOutTuple(outTuple);
        }
    }

    private void updateLeftTupleWithFiltering(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
        ElementAwareList<OutTuple_> outTupleListLeft = leftTuple.getStore(inputStoreIndexLeftOutTupleList);
        ElementAwareList<OutTuple_> rightOutList = rightTuple.getStore(inputStoreIndexRightOutTupleList);
        processOutTupleUpdate(leftTuple, rightTuple, rightOutList, outTupleListLeft, outputStoreIndexRightOutEntry);
    }

    private void updateRightTupleWithFiltering(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
        ElementAwareList<OutTuple_> outTupleListRight = rightTuple.getStore(inputStoreIndexRightOutTupleList);
        ElementAwareList<OutTuple_> leftOutList = leftTuple.getStore(inputStoreIndexLeftOutTupleList);
        processOutTupleUpdate(leftTuple, rightTuple, leftOutList, outTupleListRight, outputStoreIndexLeftOutEntry);
    }

    private void processOutTupleUpdate(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple, ElementAwareList<OutTuple_> outList,
            ElementAwareList<OutTuple_> outTupleList, int outputStoreIndexOutEntry) {
        var outTuple = findOutTuple(outTupleList, outList, outputStoreIndexOutEntry);
        if (testFiltering(leftTuple, rightTuple)) {
            if (outTuple == null) {
                insertOutTupleWithFiltering(leftTuple, rightTuple);
            } else {
                updateOutTupleLeft(outTuple, leftTuple);
            }
        } else {
            if (outTuple != null) {
                retractOutTuple(outTuple);
            }
        }
    }

    private static <Tuple_ extends AbstractTuple> Tuple_ findOutTuple(ElementAwareList<Tuple_> outTupleList,
            ElementAwareList<Tuple_> outList, int outputStoreIndexOutEntry) {
        // Hack: the outTuple has no left/right input tuple reference, use the left/right outList reference instead.
        var item = outTupleList.first();
        while (item != null) {
            // Creating list iterators here caused major GC pressure; therefore, we iterate over the entries directly.
            var outTuple = item.getElement();
            ElementAwareListEntry<Tuple_> outEntry = outTuple.getStore(outputStoreIndexOutEntry);
            var outEntryList = outEntry.getList();
            if (outList == outEntryList) {
                return outTuple;
            }
            item = item.next();
        }
        return null;
    }

    protected final void retractOutTuple(OutTuple_ outTuple) {
        ElementAwareListEntry<OutTuple_> outEntryLeft = outTuple.removeStore(outputStoreIndexLeftOutEntry);
        outEntryLeft.remove();
        ElementAwareListEntry<OutTuple_> outEntryRight = outTuple.removeStore(outputStoreIndexRightOutEntry);
        outEntryRight.remove();
        var state = outTuple.state;
        if (!state.isActive()) {
            // Impossible because they shouldn't linger in the indexes.
            throw new IllegalStateException("Impossible state: The tuple (" + outTuple.state + ") in node (" + this
                    + ") is in an unexpected state (" + outTuple.state + ").");
        }
        propagationQueue.retract(outTuple, state == TupleState.CREATING ? TupleState.ABORTING : TupleState.DYING);
    }

    @Override
    public Propagator getPropagator() {
        return propagationQueue;
    }

    @FunctionalInterface
    public interface TupleConsumer<LeftTuple_ extends AbstractTuple, Right_> extends BiConsumer<LeftTuple_, UniTuple<Right_>> {

    }

}
