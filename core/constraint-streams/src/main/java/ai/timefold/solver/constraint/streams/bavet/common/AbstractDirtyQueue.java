package ai.timefold.solver.constraint.streams.bavet.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

abstract sealed class AbstractDirtyQueue<Carrier_, Tuple_ extends AbstractTuple>
        permits FilterDirtyQueue, GenericDirtyQueue, GroupDirtyQueue, IfExistsDirtyQueue {

    private final List<Carrier_> dirtyList;
    private final List<Carrier_> insertingList;
    private final List<Carrier_> updatingList;
    private final Consumer<Tuple_> retractPropagator;
    private final Consumer<Tuple_> updatePropagator;
    private final Consumer<Tuple_> insertPropagator;

    private AbstractDirtyQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int size) {
        this.dirtyList = new ArrayList<>(size);
        // Guesstimate that inserts and updates are roughly equally common.
        this.insertingList = new ArrayList<>(size / 2);
        this.updatingList = new ArrayList<>(size / 2);
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
        // First pass; do removals and prepare inserts and updates.
        for (Carrier_ carrier : dirtyList) {
            switch (extractState(carrier)) {
                case CREATING -> insertingList.add(carrier);
                case UPDATING -> updatingList.add(carrier);
                case DYING -> propagate(carrier, retractPropagator, TupleState.DEAD);
                case ABORTING -> changeState(carrier, TupleState.DEAD);
                default -> {
                    Tuple_ tuple = extractTuple(carrier);
                    throw new IllegalStateException("Impossible state: The tuple (" + tuple + ") in node (" +
                            node + ") is in an unexpected state (" + tuple.state + ").");
                }
            }
        }
        dirtyList.clear();
        processSublist(updatingList, updatePropagator); // Second pass for updates.
        processSublist(insertingList, insertPropagator); // Third pass for inserts.
    }

    private void propagate(Carrier_ carrier, Consumer<Tuple_> propagator, TupleState tupleState) {
        Tuple_ tuple = extractTuple(carrier);
        propagator.accept(tuple);
        changeState(carrier, tupleState);
    }

    private void processSublist(List<Carrier_> list, Consumer<Tuple_> propagator) {
        if (!list.isEmpty()) {
            for (Carrier_ carrier : list) {
                processCarrier(carrier);
                propagate(carrier, propagator, TupleState.OK);
            }
            list.clear();
        }
    }

}
