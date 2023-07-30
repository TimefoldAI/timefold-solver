package ai.timefold.solver.constraint.streams.bavet.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

abstract class AbstractStaticPropagationQueue<Carrier_, Tuple_ extends AbstractTuple> {

    private final List<Carrier_> retractList;
    private final List<Carrier_> updateList;
    private final List<Carrier_> insertList;
    private final Consumer<Tuple_> retractPropagator;
    private final Consumer<Tuple_> updatePropagator;
    private final Consumer<Tuple_> insertPropagator;

    private AbstractStaticPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int size) {
        // Guesstimate that updates are the most common.
        this.retractList = new ArrayList<>(size / 10);
        this.updateList = new ArrayList<>((size / 10) * 8);
        this.insertList = new ArrayList<>(size / 10);
        // Don't create these lambdas over and over again.
        this.retractPropagator = nextNodesTupleLifecycle::retract;
        this.updatePropagator = nextNodesTupleLifecycle::update;
        this.insertPropagator = nextNodesTupleLifecycle::insert;
    }

    public AbstractStaticPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle) {
        this(nextNodesTupleLifecycle, 1000);
    }

    protected abstract Tuple_ extractTuple(Carrier_ carrier);

    protected abstract TupleState extractState(Carrier_ carrier);

    protected abstract void changeState(Carrier_ carrier, TupleState state);

    protected void processCarrier(Carrier_ carrier) {
        // Only necessary for group nodes.
    }

    public void insert(Carrier_ carrier) {
        insertList.add(carrier);
    }

    public void insert(Carrier_ carrier, TupleState state) {
        changeState(carrier, state);
        insert(carrier);
    }

    public void update(Carrier_ carrier) {
        updateList.add(carrier);
    }

    public void update(Carrier_ carrier, TupleState state) {
        changeState(carrier, state);
        update(carrier);
    }

    public void retract(Carrier_ carrier) {
        retractList.add(carrier);
    }

    public void retract(Carrier_ carrier, TupleState state) {
        changeState(carrier, state);
        retract(carrier);
    }

    public void calculateScore(AbstractNode node) {
        processRetracts(node);
        processUpdates(node);
        processInserts(node);
    }

    private void processRetracts(AbstractNode node) {
        for (Carrier_ carrier : retractList) {
            TupleState state = extractState(carrier);
            switch (state) {
                case DYING -> propagate(carrier, retractPropagator, TupleState.DEAD);
                case ABORTING -> changeState(carrier, TupleState.DEAD);
                default -> {
                    Tuple_ tuple = extractTuple(carrier);
                    throw new IllegalStateException("Impossible state: The tuple (" + tuple + ") in node (" +
                            node + ") is in an unexpected state (" + state + ").");
                }
            }
        }
        retractList.clear();
    }

    private void propagate(Carrier_ carrier, Consumer<Tuple_> propagator, TupleState tupleState) {
        Tuple_ tuple = extractTuple(carrier);
        propagator.accept(tuple);
        changeState(carrier, tupleState);
    }

    private void processUpdates(AbstractNode node) {
        process(node, updateList, TupleState.UPDATING, updatePropagator);
    }

    private void process(AbstractNode node, List<Carrier_> list, TupleState expectedState, Consumer<Tuple_> propagator) {
        for (Carrier_ carrier : list) {
            TupleState state = extractState(carrier);
            if (state != expectedState) {
                Tuple_ tuple = extractTuple(carrier);
                throw new IllegalStateException("Impossible state: The tuple (" + tuple + ") in node (" +
                        node + ") is in an unexpected state (" + state + ").");
            }
            processCarrier(carrier);
            propagate(carrier, propagator, TupleState.OK);
        }
        list.clear();
    }

    private void processInserts(AbstractNode node) {
        process(node, insertList, TupleState.CREATING, insertPropagator);
    }

}
