package ai.timefold.solver.constraint.streams.bavet.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

public final class DirtyQueue<Carrier_, Tuple_ extends AbstractTuple> implements Iterable<Carrier_> {

    public static <Tuple_ extends AbstractTuple> DirtyQueue<Tuple_, Tuple_> ofTuples() {
        return new DirtyQueue<>(Function.identity(), tuple -> tuple.state, (tuple, state) -> tuple.state = state, 1000);
    }

    private final Function<Carrier_, Tuple_> tupleGetter;
    private final Function<Carrier_, TupleState> stateGetter;
    private final BiConsumer<Carrier_, TupleState> stateSetter;
    private final Queue<Carrier_> dirtyTupleQueue;

    public DirtyQueue(Function<Carrier_, Tuple_> tupleGetter, Function<Carrier_, TupleState> stateGetter,
            BiConsumer<Carrier_, TupleState> stateSetter, int size) {
        this.tupleGetter = tupleGetter;
        this.stateGetter = stateGetter;
        this.stateSetter = stateSetter;
        dirtyTupleQueue = new ArrayDeque<>(size);
    }

    public DirtyQueue(Function<Carrier_, Tuple_> tupleGetter, Function<Carrier_, TupleState> stateGetter,
            BiConsumer<Carrier_, TupleState> stateSetter) {
        this(tupleGetter, stateGetter, stateSetter, 16);
    }

    public void insert(Carrier_ tuple) {
        dirtyTupleQueue.add(tuple);
    }

    public void insertWithState(Carrier_ tuple, TupleState state) {
        changeState(tuple, state);
        insert(tuple);
    }

    public void changeState(Carrier_ tuple, TupleState state) {
        stateSetter.accept(tuple, state);
    }

    public void clear(AbstractNode node, TupleLifecycle<Tuple_> nextNodesTupleLifecycle, Consumer<Carrier_> consumer) {
        List<Carrier_> insertList = new ArrayList<>(dirtyTupleQueue.size());
        List<Carrier_> updateList = new ArrayList<>(dirtyTupleQueue.size());
        // First pass; do removals and prepare inserts and updates.
        for (Carrier_ carrier : dirtyTupleQueue) {
            if (consumer != null) {
                consumer.accept(carrier);
            }
            switch (stateGetter.apply(carrier)) {
                case CREATING -> insertList.add(carrier);
                case UPDATING -> updateList.add(carrier);
                case DYING -> {
                    Tuple_ tuple = tupleGetter.apply(carrier);
                    nextNodesTupleLifecycle.retract(tuple);
                    stateSetter.accept(carrier, TupleState.DEAD);
                }
                case ABORTING -> stateSetter.accept(carrier, TupleState.DEAD);
                default -> {
                    Tuple_ tuple = tupleGetter.apply(carrier);
                    throw new IllegalStateException("Impossible state: The tuple (" + tuple + ") in node (" +
                            node + ") is in an unexpected state (" + tuple.state + ").");
                }
            }
        }
        // Second pass for updates.
        for (Carrier_ carrier : updateList) {
            Tuple_ tuple = tupleGetter.apply(carrier);
            nextNodesTupleLifecycle.update(tuple);
            stateSetter.accept(carrier, TupleState.OK);
        }
        // Third pass for inserts.
        for (Carrier_ carrier : insertList) {
            Tuple_ tuple = tupleGetter.apply(carrier);
            nextNodesTupleLifecycle.insert(tuple);
            stateSetter.accept(carrier, TupleState.OK);
        }
        dirtyTupleQueue.clear();
    }

    public void clear(AbstractNode node, TupleLifecycle<Tuple_> nextNodesTupleLifecycle) {
        clear(node, nextNodesTupleLifecycle, null);
    }

    @Override
    public Iterator<Carrier_> iterator() {
        return dirtyTupleQueue.iterator();
    }
}
