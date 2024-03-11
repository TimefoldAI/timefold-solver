package ai.timefold.solver.core.impl.score.stream.bavet.common;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleState;

/**
 * This implementation has the capability to move tuples between the individual propagation queues.
 * This is significantly less efficient than the {@link StaticPropagationQueue}.
 *
 * @param <Carrier_>
 * @param <Tuple_>
 */
final class DynamicPropagationQueue<Tuple_ extends AbstractTuple, Carrier_ extends AbstractPropagationMetadataCarrier<Tuple_>>
        implements PropagationQueue<Carrier_> {

    private final Consumer<Carrier_> preprocessor;
    private final List<Carrier_> dirtyList;
    private final BitSet retractQueue;
    private final BitSet insertQueue;
    private final Consumer<Tuple_> retractPropagator;
    private final Consumer<Tuple_> updatePropagator;
    private final Consumer<Tuple_> insertPropagator;

    private DynamicPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle, Consumer<Carrier_> preprocessor, int size) {
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
        // Don't create these lambdas over and over again.
        this.retractPropagator = nextNodesTupleLifecycle::retract;
        this.updatePropagator = nextNodesTupleLifecycle::update;
        this.insertPropagator = nextNodesTupleLifecycle::insert;
    }

    public DynamicPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle) {
        this(nextNodesTupleLifecycle, null);
    }

    public DynamicPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle, Consumer<Carrier_> preprocessor) {
        this(nextNodesTupleLifecycle, preprocessor, 1000);
    }

    @Override
    public void insert(Carrier_ carrier) {
        int positionInDirtyList = carrier.positionInDirtyList;
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
                    throw new IllegalStateException("Impossible state: Cannot insert (" + carrier + "), already inserting.");
            }
        }
        carrier.setState(TupleState.CREATING);
    }

    private void makeDirty(Carrier_ carrier, BitSet queue) {
        dirtyList.add(carrier);
        int position = dirtyList.size() - 1;
        queue.set(position);
        carrier.positionInDirtyList = position;
    }

    @Override
    public void update(Carrier_ carrier) {
        int positionInDirtyList = carrier.positionInDirtyList;
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
            throw new IllegalArgumentException("Impossible state: The state (" + state + ") is not a valid retract state.");
        }
        int positionInDirtyList = carrier.positionInDirtyList;
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
                    throw new IllegalStateException("Impossible state: Cannot retract (" + carrier + "), already retracting.");

            }
        }
        carrier.setState(state);
    }

    @Override
    public void propagateRetracts() {
        if (retractQueue.isEmpty()) {
            return;
        }
        int i = retractQueue.nextSetBit(0);
        while (i != -1) {
            Carrier_ carrier = dirtyList.get(i);
            TupleState state = carrier.getState();
            switch (state) {
                case DYING -> propagate(carrier, retractPropagator, TupleState.DEAD);
                case ABORTING -> clean(carrier, TupleState.DEAD);
            }
            i = retractQueue.nextSetBit(i + 1);
        }
    }

    private static <Tuple_ extends AbstractTuple, Carrier_ extends AbstractPropagationMetadataCarrier<Tuple_>> void
            propagate(Carrier_ carrier, Consumer<Tuple_> propagator, TupleState tupleState) {
        clean(carrier, tupleState); // Hide original state from the next node by doing this before propagation.
        propagator.accept(carrier.getTuple());
    }

    private static void clean(AbstractPropagationMetadataCarrier<?> carrier, TupleState tupleState) {
        carrier.setState(tupleState);
        carrier.positionInDirtyList = -1;
    }

    @Override
    public void propagateUpdates() {
        int dirtyListSize = dirtyList.size();
        BitSet insertAndRetractQueue = buildInsertAndRetractQueue(insertQueue, retractQueue);
        if (insertAndRetractQueue == null) { // Iterate over the entire list more efficiently.
            for (int i = 0; i < dirtyListSize; i++) {
                // Not using enhanced for loop in order not to create so many iterators in the hot path.
                propagateInsertOrUpdate(dirtyList.get(i), updatePropagator);
            }
        } else { // The gaps in the queue are the updates.
            int i = insertAndRetractQueue.nextClearBit(0);
            while (i != -1 && i < dirtyListSize) {
                propagateInsertOrUpdate(dirtyList.get(i), updatePropagator);
                i = insertAndRetractQueue.nextClearBit(i + 1);
            }
        }
    }

    private static BitSet buildInsertAndRetractQueue(BitSet insertQueue, BitSet retractQueue) {
        boolean noInserts = insertQueue.isEmpty();
        boolean noRetracts = retractQueue.isEmpty();
        if (noInserts && noRetracts) {
            return null;
        } else if (noInserts) {
            return retractQueue;
        } else if (noRetracts) {
            return insertQueue;
        } else {
            BitSet updateQueue = new BitSet();
            updateQueue.or(insertQueue);
            updateQueue.or(retractQueue);
            return updateQueue;
        }
    }

    @Override
    public void propagateInserts() {
        if (!insertQueue.isEmpty()) {
            int i = insertQueue.nextSetBit(0);
            while (i != -1) {
                propagateInsertOrUpdate(dirtyList.get(i), insertPropagator);
                i = insertQueue.nextSetBit(i + 1);
            }
            insertQueue.clear();
        }
        retractQueue.clear();
        dirtyList.clear();
    }

    /**
     * Exists so that implementations can customize the update/insert propagation.
     *
     * @param carrier never null
     * @param propagator never null
     */
    private void propagateInsertOrUpdate(Carrier_ carrier, Consumer<Tuple_> propagator) {
        if (preprocessor != null) {
            preprocessor.accept(carrier);
        }
        propagate(carrier, propagator, TupleState.OK);
    }

}
