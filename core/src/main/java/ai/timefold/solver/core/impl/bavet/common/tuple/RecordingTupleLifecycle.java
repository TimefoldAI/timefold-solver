package ai.timefold.solver.core.impl.bavet.common.tuple;

import ai.timefold.solver.core.impl.bavet.common.TupleRecorder;

public final class RecordingTupleLifecycle<Tuple_ extends AbstractTuple> implements TupleLifecycle<Tuple_> {
    private final TupleRecorder<Tuple_> tupleRecorder;

    public RecordingTupleLifecycle(TupleRecorder<Tuple_> tupleRecorder) {
        this.tupleRecorder = tupleRecorder;
    }

    public TupleRecorder<Tuple_> getTupleRecorder() {
        return tupleRecorder;
    }

    @Override
    public void insert(Tuple_ tuple) {
        if (tupleRecorder.isRecording()) {
            throw new IllegalStateException("Impossible state: tuple %s was inserted during recording".formatted(tuple));
        }
    }

    @Override
    public void update(Tuple_ tuple) {
        tupleRecorder.record(tuple);
    }

    @Override
    public void retract(Tuple_ tuple) {
        // Not illegal; a filter can retract a never inserted tuple on update,
        // since it does not remember what tuples it accepted to save memory
    }
}
