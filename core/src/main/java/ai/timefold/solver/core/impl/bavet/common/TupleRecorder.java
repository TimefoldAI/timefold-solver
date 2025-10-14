package ai.timefold.solver.core.impl.bavet.common;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.UnaryOperator;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record TupleRecorder<Tuple_ extends AbstractTuple>(List<Tuple_> recordedTupleList,
        UnaryOperator<Tuple_> mapper,
        IdentityHashMap<Tuple_, Tuple_> inputTupleToOutputTuple) {
    public void recordTuple(Tuple_ tuple) {
        recordedTupleList.add(inputTupleToOutputTuple.computeIfAbsent(tuple, mapper));
    }
}
