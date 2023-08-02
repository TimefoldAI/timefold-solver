package ai.timefold.solver.constraint.streams.bavet.common;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Consumer;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractPropagationMetadataCarrier.PropagationType;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

/**
 * This implementation has the capability to move tuples between the individual propagation queues.
 * This is significantly less efficient than the {@link StaticPropagationQueue}.
 *
 * @param <Carrier_>
 * @param <Tuple_>
 */
sealed abstract class AbstractDynamicPropagationQueue<Carrier_ extends AbstractPropagationMetadataCarrier, Tuple_ extends AbstractTuple>
        implements PropagationQueue<Carrier_>
        permits GroupPropagationQueue, IfExistsPropagationQueue {

    private final List<Carrier_> dirtyList;
    private final BitSet retractQueue;
    private final BitSet insertQueue;
    private final Consumer<Tuple_> retractPropagator;
    private final Consumer<Tuple_> updatePropagator;
    private final Consumer<Tuple_> insertPropagator;

    private AbstractDynamicPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int size) {
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

    public AbstractDynamicPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle) {
        this(nextNodesTupleLifecycle, 1000);
    }

    @Override
    public void insert(Carrier_ carrier) {
        PropagationType previousPropagationType = carrier.propagationType;
        carrier.propagationType = PropagationType.INSERT;
        switch (previousPropagationType) {
            case NONE -> makeDirty(carrier, insertQueue);
            case UPDATE -> insertQueue.set(carrier.positionInDirtyList);
            case RETRACT -> {
                int currentPosition = carrier.positionInDirtyList;
                retractQueue.clear(currentPosition);
                insertQueue.set(currentPosition);
            }
            default ->
                throw new IllegalStateException(
                        "Impossible state: The propagationType (" + previousPropagationType + ") is not implemented.");
        }
        changeState(carrier, TupleState.CREATING);
    }

    private void makeDirty(Carrier_ carrier, BitSet queue) {
        dirtyList.add(carrier);
        int position = dirtyList.size() - 1;
        queue.set(position);
        carrier.positionInDirtyList = position;
    }

    protected abstract void changeState(Carrier_ carrier, TupleState state);

    @Override
    public void update(Carrier_ carrier) {
        PropagationType previousPropagationType = carrier.propagationType;
        carrier.propagationType = PropagationType.UPDATE;
        switch (previousPropagationType) {
            case NONE -> {
                dirtyList.add(carrier);
                carrier.positionInDirtyList = dirtyList.size() - 1;
            }
            case INSERT -> insertQueue.clear(carrier.positionInDirtyList);
            case RETRACT -> retractQueue.clear(carrier.positionInDirtyList);
            default ->
                throw new IllegalStateException(
                        "Impossible state: The propagationType (" + previousPropagationType + ") is not implemented.");
        }
        changeState(carrier, TupleState.UPDATING);
    }

    @Override
    public void retract(Carrier_ carrier, TupleState state) {
        if (state.isActive() || state == TupleState.DEAD) {
            throw new IllegalStateException("Impossible state: The state (" + state + ") is not a valid retract state.");
        }
        PropagationType previousPropagationType = carrier.propagationType;
        carrier.propagationType = PropagationType.RETRACT;
        switch (previousPropagationType) {
            case NONE -> makeDirty(carrier, retractQueue);
            case INSERT -> {
                int currentPosition = carrier.positionInDirtyList;
                insertQueue.clear(currentPosition);
                retractQueue.set(currentPosition);
            }
            case UPDATE -> retractQueue.set(carrier.positionInDirtyList);
            default ->
                throw new IllegalStateException(
                        "Impossible state: The propagationType (" + previousPropagationType + ") is not implemented.");
        }
        changeState(carrier, state);
    }

    @Override
    public void propagateAndClear() {
        if (dirtyList.isEmpty()) {
            return;
        }
        processRetracts();
        processUpdates();
        processInserts();
        dirtyList.clear();
        retractQueue.clear();
        insertQueue.clear();
    }

    private void processRetracts() {
        if (retractQueue.isEmpty()) {
            return;
        }
        int i = retractQueue.nextSetBit(0);
        while (i != -1) {
            Carrier_ carrier = dirtyList.get(i);
            TupleState state = extractState(carrier);
            switch (state) {
                case DYING -> propagate(carrier, retractPropagator, TupleState.DEAD);
                case ABORTING -> clean(carrier, TupleState.DEAD);
            }
            i = retractQueue.nextSetBit(i + 1);
        }
    }

    protected abstract TupleState extractState(Carrier_ carrier);

    private void clean(Carrier_ carrier, TupleState tupleState) {
        changeState(carrier, tupleState);
        carrier.clearMetadata();
    }

    protected abstract Tuple_ extractTuple(Carrier_ carrier);

    private void propagate(Carrier_ carrier, Consumer<Tuple_> propagator, TupleState tupleState) {
        clean(carrier, tupleState); // Hide original state from the next node by doing this before propagation.
        propagator.accept(extractTuple(carrier));
    }

    private void processUpdates() {
        BitSet insertAndRetractQueue = buildInsertAndRetractQueue();
        if (insertAndRetractQueue == null) { // Iterate over the entire list more efficiently.
            for (Carrier_ carrier : dirtyList) {
                propagateInsertOrUpdate(carrier, updatePropagator);
            }
        } else { // The gaps in the queue are the updates.
            int dirtyListSize = dirtyList.size();
            int i = insertAndRetractQueue.nextClearBit(0);
            while (i != -1 && i < dirtyListSize) {
                propagateInsertOrUpdate(dirtyList.get(i), updatePropagator);
                i = insertAndRetractQueue.nextClearBit(i + 1);
            }
        }
    }

    private BitSet buildInsertAndRetractQueue() {
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

    /**
     * Exists so that implementations can customize the update/insert propagation.
     *
     * @param carrier never null
     * @param propagator never null
     */
    protected void propagateInsertOrUpdate(Carrier_ carrier, Consumer<Tuple_> propagator) {
        propagate(carrier, propagator, TupleState.OK);
    }

    private void processInserts() {
        if (insertQueue.isEmpty()) {
            return;
        }
        int i = insertQueue.nextSetBit(0);
        while (i != -1) {
            propagateInsertOrUpdate(dirtyList.get(i), insertPropagator);
            i = insertQueue.nextSetBit(i + 1);
        }
    }

}
