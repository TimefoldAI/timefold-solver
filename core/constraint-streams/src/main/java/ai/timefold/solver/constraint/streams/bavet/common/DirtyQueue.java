package ai.timefold.solver.constraint.streams.bavet.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

public final class DirtyQueue<Carrier_, Tuple_ extends AbstractTuple> {

    public static <Tuple_ extends AbstractTuple> DirtyQueue<Tuple_, Tuple_>
            ofTuples(TupleLifecycle<Tuple_> nextNodesTupleLifecycle) {
        return new DirtyQueue<>(nextNodesTupleLifecycle, null, tuple -> tuple.state,
                (tuple, state) -> tuple.state = state, 1000);
    }

    /**
     * Propagates tuples downstream.
     * Calls for example {@link AbstractScorer#insert(AbstractTuple)} and/or ...
     */
    private final TupleLifecycle<Tuple_> nextNodesTupleLifecycle;
    private final Function<Carrier_, Tuple_> tupleExtractor;
    private final Function<Carrier_, TupleState> stateGetter;
    private final BiConsumer<Carrier_, TupleState> stateSetter;
    private final Consumer<Carrier_> carrierProcessor;
    private final List<Carrier_> dirtyList;
    private final List<Carrier_> insertingList;
    private final List<Carrier_> updatingList;

    private DirtyQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle, Function<Carrier_, Tuple_> tupleExtractor,
            Function<Carrier_, TupleState> stateGetter, BiConsumer<Carrier_, TupleState> stateSetter,
            Consumer<Carrier_> carrierProcessor, int size) {
        this.nextNodesTupleLifecycle = Objects.requireNonNull(nextNodesTupleLifecycle);
        this.tupleExtractor = tupleExtractor; // Null if the tuple is the carrier.
        this.stateGetter = Objects.requireNonNull(stateGetter);
        this.stateSetter = Objects.requireNonNull(stateSetter);
        this.carrierProcessor = carrierProcessor; // Only required for group nodes.
        this.dirtyList = new ArrayList<>(size);
        // Guesstimate that inserts and updates are roughly equally common.
        this.insertingList = new ArrayList<>(size / 2);
        this.updatingList = new ArrayList<>(size / 2);
    }

    public DirtyQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle, Function<Carrier_, Tuple_> tupleExtractor,
            Function<Carrier_, TupleState> stateGetter, BiConsumer<Carrier_, TupleState> stateSetter, int size) {
        this(nextNodesTupleLifecycle, tupleExtractor, stateGetter, stateSetter, null, size);
    }

    public DirtyQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle, Function<Carrier_, Tuple_> tupleExtractor,
            Function<Carrier_, TupleState> stateGetter, BiConsumer<Carrier_, TupleState> stateSetter,
            Consumer<Carrier_> carrierProcessor) {
        this(nextNodesTupleLifecycle, tupleExtractor, stateGetter, stateSetter, carrierProcessor, 1000);
    }

    public DirtyQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle, Function<Carrier_, Tuple_> tupleExtractor,
            Function<Carrier_, TupleState> stateGetter, BiConsumer<Carrier_, TupleState> stateSetter) {
        this(nextNodesTupleLifecycle, tupleExtractor, stateGetter, stateSetter, null);
    }

    public void insert(Carrier_ carrier) {
        dirtyList.add(carrier);
    }

    public void insertWithState(Carrier_ carrier, TupleState state) {
        changeState(carrier, state);
        insert(carrier);
    }

    public void changeState(Carrier_ carrier, TupleState state) {
        stateSetter.accept(carrier, state);
    }

    public void calculateScore(AbstractNode node) {
        if (dirtyList.isEmpty()) {
            return;
        }
        // First pass; do removals and prepare inserts and updates.
        for (Carrier_ carrier : dirtyList) {
            switch (stateGetter.apply(carrier)) {
                case CREATING -> insertingList.add(carrier);
                case UPDATING -> updatingList.add(carrier);
                case DYING -> propagate(carrier, nextNodesTupleLifecycle::retract, TupleState.DEAD);
                case ABORTING -> stateSetter.accept(carrier, TupleState.DEAD);
                default -> {
                    Tuple_ tuple = tupleExtractor.apply(carrier);
                    throw new IllegalStateException("Impossible state: The tuple (" + tuple + ") in node (" +
                            node + ") is in an unexpected state (" + tuple.state + ").");
                }
            }
        }
        dirtyList.clear();
        processSublist(updatingList, nextNodesTupleLifecycle::update); // Second pass for updates.
        processSublist(insertingList, nextNodesTupleLifecycle::insert); // Third pass for inserts.
    }

    private void propagate(Carrier_ carrier, Consumer<Tuple_> propagator, TupleState tupleState) {
        Tuple_ tuple = tupleExtractor == null ? (Tuple_) carrier : tupleExtractor.apply(carrier);
        propagator.accept(tuple);
        stateSetter.accept(carrier, tupleState);
    }

    private void processSublist(List<Carrier_> list, Consumer<Tuple_> propagator) {
        if (!list.isEmpty()) {
            boolean hasProcessor = carrierProcessor != null;
            for (Carrier_ carrier : list) {
                if (hasProcessor) {
                    carrierProcessor.accept(carrier);
                }
                propagate(carrier, propagator, TupleState.OK);
            }
            list.clear();
        }
    }

}
