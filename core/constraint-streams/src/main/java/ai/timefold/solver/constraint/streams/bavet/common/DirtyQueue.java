package ai.timefold.solver.constraint.streams.bavet.common;

import java.util.ArrayDeque;
import java.util.Iterator;
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
        for (Carrier_ carrier : dirtyTupleQueue) {
            Tuple_ tuple = tupleGetter.apply(carrier);
            if (consumer != null) {
                consumer.accept(carrier);
            }
            switch (stateGetter.apply(carrier)) {
                case CREATING -> {
                    nextNodesTupleLifecycle.insert(tuple);
                    stateSetter.accept(carrier, TupleState.OK);
                }
                case UPDATING -> {
                    nextNodesTupleLifecycle.update(tuple);
                    stateSetter.accept(carrier, TupleState.OK);
                }
                case DYING -> {
                    nextNodesTupleLifecycle.retract(tuple);
                    stateSetter.accept(carrier, TupleState.DEAD);
                }
                case ABORTING -> stateSetter.accept(carrier, TupleState.DEAD);
                default -> throw new IllegalStateException("Impossible state: The tuple (" + tuple + ") in node (" +
                        node + ") is in an unexpected state (" + tuple.state + ").");
            }
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
