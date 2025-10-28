package ai.timefold.solver.core.impl.bavet.common.tuple;

import ai.timefold.solver.core.impl.bavet.common.TupleRecorder;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class RecordingTupleLifecycle<Tuple_ extends AbstractTuple> implements TupleLifecycle<Tuple_>, AutoCloseable {
    @Nullable
    TupleRecorder<Tuple_> tupleRecorder;

    public RecordingTupleLifecycle<Tuple_> recordInto(TupleRecorder<Tuple_> tupleRecorder) {
        this.tupleRecorder = tupleRecorder;
        return this;
    }

    @Override
    public void close() {
        this.tupleRecorder = null;
    }

    @Override
    public void insert(Tuple_ tuple) {
        if (tupleRecorder != null) {
            throw new IllegalStateException("""
                    Illegal state: tuple %s was inserted during recording.
                    Certain operations like flattenLast will create new tuples
                    on update if its mapping function returns a new instance.
                    Maybe refactor the code to avoid creating new instances,
                    or avoid using precompute if that is not possible.
                    """.formatted(tuple));
        }
    }

    @Override
    public void update(Tuple_ tuple) {
        if (tupleRecorder != null) {
            tupleRecorder.recordTuple(tuple);
        }
    }

    @Override
    public void retract(Tuple_ tuple) {
        // Not illegal; a filter can retract a never inserted tuple on update,
        // since it does not remember what tuples it accepted to save memory
    }
}
