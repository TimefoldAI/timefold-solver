package ai.timefold.solver.core.impl.bavet.common;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;

/**
 * The implementation moves tuples directly into an either retract, update or insert queue,
 * without any option of moving between the queues.
 * This is the most efficient implementation.
 * It will throw exceptions if a tuple is in the wrong queue, based on its state.
 *
 * @param <Tuple_>
 */
public final class StaticPropagationQueue<Tuple_ extends Tuple>
        implements PropagationQueue<Tuple_> {

    private final Deque<Tuple_> retractQueue;
    private final Deque<Tuple_> updateQueue;
    private final Deque<Tuple_> insertQueue;
    private final TupleLifecycle<Tuple_> nextNodesTupleLifecycle;

    public StaticPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int size) {
        // Guesstimate that updates are dominant.
        this.retractQueue = new ArrayDeque<>(size / 20);
        this.updateQueue = new ArrayDeque<>((size / 20) * 18);
        this.insertQueue = new ArrayDeque<>(size / 20);
        this.nextNodesTupleLifecycle = nextNodesTupleLifecycle;
    }

    public StaticPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle) {
        this(nextNodesTupleLifecycle, 1000);
    }

    @Override
    public void insert(Tuple_ carrier) {
        if (carrier.getState() == TupleState.CREATING) {
            throw new IllegalStateException("Impossible state: The tuple (%s) is already in the insert queue."
                    .formatted(carrier));
        }
        carrier.setState(TupleState.CREATING);
        insertQueue.add(carrier);
    }

    @Override
    public void update(Tuple_ carrier) {
        if (carrier.getState() == TupleState.UPDATING) { // Skip double updates.
            return;
        }
        carrier.setState(TupleState.UPDATING);
        updateQueue.add(carrier);
    }

    @Override
    public void retract(Tuple_ carrier, TupleState state) {
        var carrierState = carrier.getState();
        if (carrierState == state) { // Skip double retracts.
            return;
        }
        if (state.isActive() || state == TupleState.DEAD) {
            throw new IllegalArgumentException("Impossible state: The state (%s) is not a valid retract state."
                    .formatted(state));
        } else if (carrierState == TupleState.ABORTING || carrierState == TupleState.DYING) {
            throw new IllegalStateException("Impossible state: The tuple (%s) is already in the retract queue."
                    .formatted(carrier));
        }
        carrier.setState(state);
        retractQueue.add(carrier);
    }

    @Override
    public void propagateRetracts() {
        while (!retractQueue.isEmpty()) {
            var tuple = retractQueue.poll();
            switch (tuple.getState()) {
                case DYING -> {
                    // Change state before propagation, so that the next node can't make decisions on the original state.
                    tuple.setState(TupleState.DEAD);
                    nextNodesTupleLifecycle.retract(tuple);
                }
                case ABORTING -> tuple.setState(TupleState.DEAD);
            }
        }
    }

    @Override
    public void propagateUpdates() {
        processAndClear(updateQueue, nextNodesTupleLifecycle::update);
    }

    private static <Tuple_ extends AbstractTuple> void processAndClear(Deque<Tuple_> dirtyQueue,
            Consumer<Tuple_> tupleLifecycle) {
        while (!dirtyQueue.isEmpty()) {
            var tuple = dirtyQueue.poll();
            if (tuple.getState() == TupleState.DEAD) {
                // DEAD signifies the tuple was both in insert/update and retract queues.
                // This happens when a tuple was inserted/updated and subsequently retracted, all before propagation.
                // We can safely ignore the later insert/update,
                // as by this point the more recent retract has already been processed,
                // setting the state to DEAD.
                continue;
            }
            // Change state before propagation, so that the next node can't make decisions on the original state.
            tuple.setState(TupleState.OK);
            tupleLifecycle.accept(tuple);
        }
    }

    @Override
    public void propagateInserts() {
        processAndClear(insertQueue, nextNodesTupleLifecycle::insert);
        if (!retractQueue.isEmpty()) {
            throw new IllegalStateException("Impossible state: The retract queue (%s) is not empty."
                    .formatted(retractQueue));
        } else if (!updateQueue.isEmpty()) {
            throw new IllegalStateException("Impossible state: The update queue (%s) is not empty."
                    .formatted(updateQueue));
        }
    }

}
