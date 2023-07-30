package ai.timefold.solver.constraint.streams.bavet.common;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Consumer;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

/**
 * This implementation has the capability to move tuples between the individual propagation queues.
 * This is significantly less efficient than the {@link AbstractStaticPropagationQueue}.
 *
 * @param <Carrier_>
 * @param <Tuple_>
 */
public sealed abstract class AbstractDynamicPropagationQueue<Carrier_ extends AbstractPropagationMetadataCarrier, Tuple_ extends AbstractTuple>
        implements PropagationQueue<Carrier_>
        permits GroupPropagationQueue, IfExistsPropagationQueue {

    private final List<Carrier_> dirtyList;
    private final BitSet retractQueue;
    private final BitSet updateQueue;
    private final BitSet insertQueue;
    private final Consumer<Tuple_> retractPropagator;
    private final Consumer<Tuple_> updatePropagator;
    private final Consumer<Tuple_> insertPropagator;

    private AbstractDynamicPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int size) {
        this.dirtyList = new ArrayList<>(size);
        // Guesstimate that updates are the most common.
        this.retractQueue = new BitSet(size / 10);
        this.updateQueue = new BitSet((size / 10) * 8);
        this.insertQueue = new BitSet(size / 10);
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
        replace(carrier, TupleState.CREATING, insertQueue);
    }

    private void replace(Carrier_ carrier, TupleState state, BitSet newQueue) {
        BitSet currentQueue = carrier.currentQueue;
        if (currentQueue == null) { // This item is in no queue yet.
            dirtyList.add(carrier);
            int position = dirtyList.size() - 1;
            newQueue.set(position);
            carrier.currentQueue = newQueue;
            carrier.positionInDirtyList = position;
        } else if (currentQueue != newQueue) { // Only move items between queues if necessary.
            int position = carrier.positionInDirtyList;
            currentQueue.clear(position);
            newQueue.set(position);
            carrier.currentQueue = newQueue;
        }
        changeState(carrier, state);
    }

    protected abstract void changeState(Carrier_ carrier, TupleState state);

    @Override
    public void update(Carrier_ carrier) {
        replace(carrier, TupleState.UPDATING, updateQueue);
    }

    @Override
    public void retract(Carrier_ carrier, TupleState state) {
        replace(carrier, state, retractQueue);
    }

    @Override
    public void calculateScore(AbstractNode node) {
        processRetracts(node);
        processUpdates(node);
        processInserts(node);
        dirtyList.clear();
    }

    private void processRetracts(AbstractNode node) {
        if (retractQueue.isEmpty()) {
            return;
        }
        for (int i = retractQueue.nextSetBit(0); i != -1; i = retractQueue.nextSetBit(i + 1)) {
            Carrier_ carrier = dirtyList.get(i);
            TupleState state = extractState(carrier);
            Tuple_ tuple = extractTuple(carrier);
            switch (state) {
                case DYING -> propagate(carrier, retractPropagator, TupleState.DEAD);
                case ABORTING -> {
                    changeState(carrier, TupleState.DEAD);
                    clearMetadata(carrier);
                }
                default -> throw new IllegalStateException("Impossible state: The tuple (" + tuple + ") in node (" + node
                        + ") is in an unexpected state (" + state + ").");
            }
        }
        retractQueue.clear();
    }

    protected abstract TupleState extractState(Carrier_ carrier);

    protected abstract Tuple_ extractTuple(Carrier_ carrier);

    private void clearMetadata(Carrier_ carrier) {
        carrier.currentQueue = null;
        carrier.positionInDirtyList = -1;
    }

    private void propagate(Carrier_ carrier, Consumer<Tuple_> propagator, TupleState tupleState) {
        Tuple_ tuple = extractTuple(carrier);
        propagator.accept(tuple);
        changeState(carrier, tupleState);
        clearMetadata(carrier);
    }

    private void processUpdates(AbstractNode node) {
        process(node, updateQueue, TupleState.UPDATING, updatePropagator);
    }

    private void process(AbstractNode node, BitSet queue, TupleState expectedState, Consumer<Tuple_> propagator) {
        if (queue.isEmpty()) {
            return;
        }
        for (int i = queue.nextSetBit(0); i != -1; i = queue.nextSetBit(i + 1)) {
            Carrier_ carrier = dirtyList.get(i);
            TupleState state = extractState(carrier);
            if (state != expectedState) {
                Tuple_ tuple = extractTuple(carrier);
                throw new IllegalStateException("Impossible state: The tuple (" + tuple + ") in node (" +
                        node + ") is in an unexpected state (" + state + ").");
            }
            processCarrier(carrier);
            propagate(carrier, propagator, TupleState.OK);
        }
        queue.clear();
    }

    protected void processCarrier(Carrier_ carrier) {
        // Only necessary for group nodes.
    }

    private void processInserts(AbstractNode node) {
        process(node, insertQueue, TupleState.CREATING, insertPropagator);
    }

}
