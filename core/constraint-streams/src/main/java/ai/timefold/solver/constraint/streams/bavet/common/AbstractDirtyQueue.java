package ai.timefold.solver.constraint.streams.bavet.common;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Consumer;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

abstract sealed class AbstractDirtyQueue<Carrier_, Tuple_ extends AbstractTuple>
        permits GroupDirtyQueue, IfExistsDirtyQueue {

    private final List<Carrier_> dirtyList;
    private final BitSet updatingIndicesSet;
    private final BitSet insertingIndicesSet;
    private final Consumer<Tuple_> retractPropagator;
    private final Consumer<Tuple_> updatePropagator;
    private final Consumer<Tuple_> insertPropagator;

    private AbstractDirtyQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int size) {
        this.dirtyList = new ArrayList<>(size);
        // Guesstimate that updates are the most common.
        this.updatingIndicesSet = new BitSet((size / 10) * 8);
        this.insertingIndicesSet = new BitSet(size / 10);
        // Don't create these lambdas over and over again.
        this.retractPropagator = nextNodesTupleLifecycle::retract;
        this.updatePropagator = nextNodesTupleLifecycle::update;
        this.insertPropagator = nextNodesTupleLifecycle::insert;
    }

    public AbstractDirtyQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle) {
        this(nextNodesTupleLifecycle, 1000);
    }

    protected abstract Tuple_ extractTuple(Carrier_ carrier);

    protected abstract TupleState extractState(Carrier_ carrier);

    public abstract void changeState(Carrier_ carrier, TupleState state);

    protected void processCarrier(Carrier_ carrier) {
        // Only necessary for group nodes.
    }

    public void insert(Carrier_ carrier) {
        dirtyList.add(carrier);
    }

    public void insertWithState(Carrier_ carrier, TupleState state) {
        changeState(carrier, state);
        insert(carrier);
    }

    public void calculateScore(AbstractNode node) {
        if (dirtyList.isEmpty()) {
            return;
        }
        processRetracts(node); // First pass; do removals and prepare inserts and updates.
        processUpdates(); // Second pass for updates.
        processInserts(); // Third pass for inserts.
        dirtyList.clear();
    }

    private void processRetracts(AbstractNode node) {
        int size = dirtyList.size();
        for (int i = 0; i < size; i++) {
            Carrier_ carrier = dirtyList.get(i);
            TupleState state = extractState(carrier);
            switch (state) {
                case CREATING -> insertingIndicesSet.set(i);
                case UPDATING -> updatingIndicesSet.set(i);
                case DYING -> propagate(carrier, retractPropagator, TupleState.DEAD);
                case ABORTING -> changeState(carrier, TupleState.DEAD);
                default -> {
                    Tuple_ tuple = extractTuple(carrier);
                    throw new IllegalStateException("Impossible state: The tuple (" + tuple + ") in node (" +
                            node + ") is in an unexpected state (" + tuple.state + ").");
                }
            }
        }
    }

    private void propagate(Carrier_ carrier, Consumer<Tuple_> propagator, TupleState tupleState) {
        Tuple_ tuple = extractTuple(carrier);
        propagator.accept(tuple);
        changeState(carrier, tupleState);
    }

    private void processUpdates() {
        processSublist(updatingIndicesSet, updatePropagator);
    }

    private void processInserts() {
        processSublist(insertingIndicesSet, insertPropagator);
    }

    private void processSublist(BitSet indicesToProcess, Consumer<Tuple_> propagator) {
        if (!indicesToProcess.isEmpty()) {
            for (int i = indicesToProcess.nextSetBit(0); i != -1; i = indicesToProcess.nextSetBit(i + 1)) {
                Carrier_ carrier = dirtyList.get(i);
                processCarrier(carrier);
                propagate(carrier, propagator, TupleState.OK);
            }
            indicesToProcess.clear();
        }
    }

}
