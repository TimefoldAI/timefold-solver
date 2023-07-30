package ai.timefold.solver.constraint.streams.bavet.common;

import java.util.ArrayList;
import java.util.List;
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
public sealed abstract class AbstractStaticPropagationQueue<Tuple_ extends AbstractTuple>
        implements PropagationQueue<Tuple_>
        permits FilterPropagationQueue, GenericPropagationQueue {

    private final List<Tuple_> retractList;
    private final List<Tuple_> updateList;
    private final List<Tuple_> insertList;
    private final Consumer<Tuple_> retractPropagator;
    private final Consumer<Tuple_> updatePropagator;
    private final Consumer<Tuple_> insertPropagator;

    protected AbstractStaticPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int size) {
        // Guesstimate that updates are the most common.
        this.retractList = new ArrayList<>(size / 10);
        this.updateList = new ArrayList<>((size / 10) * 8);
        this.insertList = new ArrayList<>(size / 10);
        // Don't create these lambdas over and over again.
        this.retractPropagator = nextNodesTupleLifecycle::retract;
        this.updatePropagator = nextNodesTupleLifecycle::update;
        this.insertPropagator = nextNodesTupleLifecycle::insert;
    }

    protected AbstractStaticPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle) {
        this(nextNodesTupleLifecycle, 1000);
    }

    protected abstract TupleState extractState(Tuple_ carrier);

    protected abstract void changeState(Tuple_ carrier, TupleState state);

    @Override
    public void insert(Tuple_ carrier) {
        changeState(carrier, TupleState.CREATING);
        insertList.add(carrier);
    }

    @Override
    public void update(Tuple_ carrier) {
        changeState(carrier, TupleState.UPDATING);
        updateList.add(carrier);
    }

    @Override
    public void retract(Tuple_ carrier, TupleState state) {
        changeState(carrier, state);
        retractList.add(carrier);
    }

    @Override
    public void calculateScore(AbstractNode node) {
        processRetracts(node);
        processUpdates(node);
        processInserts(node);
    }

    private void processRetracts(AbstractNode node) {
        for (Tuple_ tuple : retractList) {
            TupleState state = extractState(tuple);
            switch (state) {
                case DYING -> propagate(tuple, retractPropagator, TupleState.DEAD);
                case ABORTING -> changeState(tuple, TupleState.DEAD);
                default -> {
                    throw new IllegalStateException("Impossible state: The tuple (" + tuple + ") in node (" +
                            node + ") is in an unexpected state (" + state + ").");
                }
            }
        }
        retractList.clear();
    }

    private void propagate(Tuple_ tuple, Consumer<Tuple_> propagator, TupleState tupleState) {
        propagator.accept(tuple);
        changeState(tuple, tupleState);
    }

    private void processUpdates(AbstractNode node) {
        process(node, updateList, TupleState.UPDATING, updatePropagator);
    }

    private void process(AbstractNode node, List<Tuple_> list, TupleState expectedState, Consumer<Tuple_> propagator) {
        for (Tuple_ tuple : list) {
            TupleState state = extractState(tuple);
            if (state == TupleState.DEAD) {
                /*
                 * DEAD is allowed, as that signifies the tuple was both in insert/update and retract queues.
                 * This happens when a tuple was inserted/updated and subsequently retracted, all before propagation.
                 * We can safely ignore the later insert/update,
                 * as by this point the more recent retract has already been processed,
                 * as signified by the DEAD state; the tuple already died or was aborted.
                 */
                continue;
            } else if (state != expectedState) {
                throw new IllegalStateException("Impossible state: The tuple (" + tuple + ") in node (" +
                        node + ") is in an unexpected state (" + state + ").");
            }
            propagate(tuple, propagator, TupleState.OK);
        }
        list.clear();
    }

    private void processInserts(AbstractNode node) {
        process(node, insertList, TupleState.CREATING, insertPropagator);
    }

}
