package ai.timefold.solver.core.impl.bavet.common;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;

/**
 * This implementation has the capability to move tuples between the individual propagation queues.
 * This is significantly less efficient than the {@link StaticPropagationQueue}.
 *
 * @param <Carrier_>
 * @param <Tuple_>
 */
final class DynamicPropagationQueue<Tuple_ extends AbstractTuple, Carrier_ extends AbstractPropagationMetadataCarrier<Tuple_>>
        implements PropagationQueue<Carrier_> {

    private static final int INITIAL_CAPACITY = 1000; // Selected arbitrarily.

    private final Consumer<Carrier_> preprocessor;
    private final List<Carrier_> dirtyList;
    private final BitSet retractQueue;
    private final BitSet insertQueue;
    private final TupleLifecycle<Tuple_> nextNodesTupleLifecycle;

    private DynamicPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle, Consumer<Carrier_> preprocessor,
            int size) {
        this.preprocessor = preprocessor;
        /*
         * All dirty carriers are stored in a list, never moved, never removed unless after propagation.
         * Their unchanging position in the list is their index for the bitset-based queues.
         * This way, we can cheaply move them between the queues.
         */
        this.dirtyList = new ArrayList<>(size);
        // Updates tend to be dominant; update queue isn't stored, it's deduced as neither insert nor retract.
        this.retractQueue = new BitSet(size);
        this.insertQueue = new BitSet(size);
        this.nextNodesTupleLifecycle = nextNodesTupleLifecycle;
    }

    public DynamicPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle) {
        this(nextNodesTupleLifecycle, null, INITIAL_CAPACITY);
    }

    public DynamicPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle, Consumer<Carrier_> preprocessor) {
        this(nextNodesTupleLifecycle, preprocessor, INITIAL_CAPACITY);
    }

    @Override
    public void insert(Carrier_ carrier) {
        var positionInDirtyList = carrier.positionInDirtyList;
        if (positionInDirtyList < 0) {
            makeDirty(carrier, insertQueue);
        } else {
            switch (carrier.getState()) {
                case UPDATING -> insertQueue.set(positionInDirtyList);
                case ABORTING, DYING -> {
                    retractQueue.clear(positionInDirtyList);
                    insertQueue.set(positionInDirtyList);
                }
                default ->
                    throw new IllegalStateException("Impossible state: Cannot insert (%s), already inserting."
                            .formatted(carrier));
            }
        }
        carrier.setState(TupleState.CREATING);
    }

    private void makeDirty(Carrier_ carrier, BitSet queue) {
        dirtyList.add(carrier);
        var position = dirtyList.size() - 1;
        queue.set(position);
        carrier.positionInDirtyList = position;
    }

    @Override
    public void update(Carrier_ carrier) {
        var positionInDirtyList = carrier.positionInDirtyList;
        if (positionInDirtyList < 0) {
            dirtyList.add(carrier);
            carrier.positionInDirtyList = dirtyList.size() - 1;
        } else {
            switch (carrier.getState()) {
                case CREATING -> insertQueue.clear(positionInDirtyList);
                case ABORTING, DYING -> retractQueue.clear(positionInDirtyList);
                default -> {
                    // Skip double updates.
                }
            }
        }
        carrier.setState(TupleState.UPDATING);
    }

    @Override
    public void retract(Carrier_ carrier, TupleState state) {
        if (state.isActive() || state == TupleState.DEAD) {
            throw new IllegalArgumentException("Impossible state: The state (%s) is not a valid retract state."
                    .formatted(state));
        }
        var positionInDirtyList = carrier.positionInDirtyList;
        if (positionInDirtyList < 0) {
            makeDirty(carrier, retractQueue);
        } else {
            switch (carrier.getState()) {
                case CREATING -> {
                    insertQueue.clear(positionInDirtyList);
                    retractQueue.set(positionInDirtyList);
                }
                case UPDATING -> retractQueue.set(positionInDirtyList);
                default ->
                    throw new IllegalStateException("Impossible state: Cannot retract (%s), already retracting."
                            .formatted(carrier));

            }
        }
        carrier.setState(state);
    }

    @Override
    public void propagateRetracts() {
        if (retractQueue.isEmpty()) {
            return;
        }
        var i = retractQueue.nextSetBit(0);
        while (i != -1) {
            var carrier = dirtyList.get(i);
            var state = carrier.getState();
            switch (state) {
                case DYING -> {
                    clean(carrier, TupleState.DEAD); // Hide original state from the next node by doing this before propagation.
                    nextNodesTupleLifecycle.retract(carrier.getTuple());
                }
                case ABORTING -> clean(carrier, TupleState.DEAD);
            }
            i = retractQueue.nextSetBit(i + 1);
        }
    }

    private static void clean(AbstractPropagationMetadataCarrier<?> carrier, TupleState tupleState) {
        carrier.setState(tupleState);
        carrier.positionInDirtyList = -1;
    }

    @Override
    public void propagateUpdates() {
        var dirtyListSize = dirtyList.size();
        var insertAndRetractQueue = buildInsertAndRetractQueue(insertQueue, retractQueue);
        if (insertAndRetractQueue == null) { // Iterate over the entire list more efficiently.
            for (var i = 0; i < dirtyListSize; i++) {
                // Not using enhanced for loop in order not to create so many iterators in the hot path.
                propagateInsertOrUpdate(dirtyList.get(i), true);
            }
        } else { // The gaps in the queue are the updates.
            var i = insertAndRetractQueue.nextClearBit(0);
            while (i != -1 && i < dirtyListSize) {
                propagateInsertOrUpdate(dirtyList.get(i), true);
                i = insertAndRetractQueue.nextClearBit(i + 1);
            }
        }
    }

    private static BitSet buildInsertAndRetractQueue(BitSet insertQueue, BitSet retractQueue) {
        var noInserts = insertQueue.isEmpty();
        var noRetracts = retractQueue.isEmpty();
        if (noInserts && noRetracts) {
            return null;
        } else if (noInserts) {
            return retractQueue;
        } else if (noRetracts) {
            return insertQueue;
        } else {
            var updateQueue = new BitSet(Math.max(insertQueue.length(), retractQueue.length()));
            updateQueue.or(insertQueue);
            updateQueue.or(retractQueue);
            return updateQueue;
        }
    }

    private void propagateInsertOrUpdate(Carrier_ carrier, boolean isUpdate) {
        if (preprocessor != null) {
            preprocessor.accept(carrier);
        }
        clean(carrier, TupleState.OK); // Hide original state from the next node by doing this before propagation.
        if (isUpdate) {
            nextNodesTupleLifecycle.update(carrier.getTuple());
        } else {
            nextNodesTupleLifecycle.insert(carrier.getTuple());
        }
    }

    @Override
    public void propagateInserts() {
        if (!insertQueue.isEmpty()) {
            var i = insertQueue.nextSetBit(0);
            while (i != -1) {
                propagateInsertOrUpdate(dirtyList.get(i), false);
                i = insertQueue.nextSetBit(i + 1);
            }
            insertQueue.clear();
        }
        retractQueue.clear();
        dirtyList.clear();
    }

}
