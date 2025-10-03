package ai.timefold.solver.core.impl.bavet.common;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;

public final class TupleRecorder<Tuple_ extends AbstractTuple> implements Consumer<Tuple_> {
    List<Tuple_> recordedTupleList;
    UnaryOperator<Tuple_> mapper;
    IdentityHashMap<Tuple_, Tuple_> inputTupleToOutputTuple = new IdentityHashMap<>();

    public void reset() {
        inputTupleToOutputTuple.clear();
    }

    public void recordingInto(List<Tuple_> recordedTupleList, UnaryOperator<Tuple_> mapper,
            Runnable runner) {
        this.recordedTupleList = recordedTupleList;
        this.mapper = mapper;
        runner.run();
        this.recordedTupleList = null;
        this.mapper = null;
    }

    @Override
    public void accept(Tuple_ tuple) {
        if (recordedTupleList != null) {
            recordedTupleList.add(inputTupleToOutputTuple.computeIfAbsent(tuple, mapper));
        }
    }
}
