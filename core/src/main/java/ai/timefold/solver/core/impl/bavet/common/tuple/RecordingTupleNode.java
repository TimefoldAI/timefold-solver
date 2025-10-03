package ai.timefold.solver.core.impl.bavet.common.tuple;

import ai.timefold.solver.core.impl.bavet.common.AbstractNode;
import ai.timefold.solver.core.impl.bavet.common.Propagator;
import ai.timefold.solver.core.impl.bavet.common.StaticPropagationQueue;
import ai.timefold.solver.core.impl.bavet.common.TupleRecorder;

public class RecordingTupleNode<Tuple_ extends AbstractTuple> extends AbstractNode implements TupleLifecycle<Tuple_> {
    private final TupleRecorder<Tuple_> tupleRecorder;

    public RecordingTupleNode(TupleRecorder<Tuple_> tupleRecorder) {
        this.tupleRecorder = tupleRecorder;
    }

    public TupleRecorder<Tuple_> getTupleRecorder() {
        return tupleRecorder;
    }

    @Override
    public void insert(Tuple_ tuple) {
        tupleRecorder.accept(tuple);
    }

    @Override
    public void update(Tuple_ tuple) {
        tupleRecorder.accept(tuple);
    }

    @Override
    public void retract(Tuple_ tuple) {
        // Do nothing
    }

    @Override
    public Propagator getPropagator() {
        return new StaticPropagationQueue<>(new TupleLifecycle<>() {
            @Override
            public void insert(AbstractTuple tuple) {
                // do nothing
            }

            @Override
            public void update(AbstractTuple tuple) {
                // do nothing
            }

            @Override
            public void retract(AbstractTuple tuple) {
                // do nothing
            }
        });
    }
}
