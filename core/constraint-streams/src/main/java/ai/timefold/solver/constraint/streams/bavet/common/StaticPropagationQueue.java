package ai.timefold.solver.constraint.streams.bavet.common;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

/**
 * The implementation moves tuples directly into an either retract, update or insert queue,
 * without any option of moving between the queues.
 * This is the most efficient implementation.
 * It will throw exceptions if a tuple is in the wrong queue, based on its state.
 *
 * @param <Tuple_>
 */
public final class StaticPropagationQueue<Tuple_ extends AbstractTuple>
        implements PropagationQueue<Tuple_> {

    private final Deque<Tuple_> retractQueue;
    private final Deque<Tuple_> updateQueue;
    private final Deque<Tuple_> insertQueue;
    private final Consumer<Tuple_> retractPropagator;
    private final Consumer<Tuple_> updatePropagator;
    private final Consumer<Tuple_> insertPropagator;

    public StaticPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int size) {
        // Guesstimate that updates are dominant.
        this.retractQueue = new ArrayDeque<>(size / 20);
        this.updateQueue = new ArrayDeque<>((size / 20) * 18);
        this.insertQueue = new ArrayDeque<>(size / 20);
        // Don't create these lambdas over and over again.
        this.retractPropagator = nextNodesTupleLifecycle::retract;
        this.updatePropagator = nextNodesTupleLifecycle::update;
        this.insertPropagator = nextNodesTupleLifecycle::insert;
    }

    public StaticPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle) {
        this(nextNodesTupleLifecycle, 1000);
    }

    @Override
    public void insert(Tuple_ carrier) {
        carrier.state = TupleState.CREATING;
        insertQueue.add(carrier);
    }

    @Override
    public void update(Tuple_ carrier) {
        carrier.state = TupleState.UPDATING;
        updateQueue.add(carrier);
    }

    @Override
    public void retract(Tuple_ carrier, TupleState state) {
        if (state.isActive() || state == TupleState.DEAD) {
            throw new IllegalStateException("Impossible state: The state (" + state + ") is not a valid retract state.");
        }
        carrier.state = state;
        retractQueue.add(carrier);
    }

    @Override
    public void propagateAndClear() {
        processRetracts();
        processUpdates();
        processInserts();
    }

    private void processRetracts() {
        if (retractQueue.isEmpty()) {
            return;
        }
        for (Tuple_ tuple : retractQueue) {
            switch (tuple.state) {
                case DYING -> propagate(tuple, retractPropagator, TupleState.DEAD);
                case ABORTING -> tuple.state = TupleState.DEAD;
            }
        }
        retractQueue.clear();
    }

    private void propagate(Tuple_ tuple, Consumer<Tuple_> propagator, TupleState tupleState) {
        propagator.accept(tuple);
        tuple.state = tupleState;
    }

    private void processUpdates() {
        process(updateQueue, updatePropagator);
    }

    private void process(Deque<Tuple_> dirtyQueue, Consumer<Tuple_> propagator) {
        if (dirtyQueue.isEmpty()) {
            return;
        }
        for (Tuple_ tuple : dirtyQueue) {
            if (tuple.state == TupleState.DEAD) {
                /*
                 * DEAD signifies the tuple was both in insert/update and retract queues.
                 * This happens when a tuple was inserted/updated and subsequently retracted, all before propagation.
                 * We can safely ignore the later insert/update,
                 * as by this point the more recent retract has already been processed,
                 * as signified by the DEAD state; the tuple already died or was aborted.
                 */
                continue;
            }
            propagate(tuple, propagator, TupleState.OK);
        }
        dirtyQueue.clear();
    }

    private void processInserts() {
        process(insertQueue, insertPropagator);
    }

}
