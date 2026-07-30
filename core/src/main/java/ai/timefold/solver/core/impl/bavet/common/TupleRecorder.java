package ai.timefold.solver.core.impl.bavet.common;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.UnaryOperator;

import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.indictment.IndictmentSource;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record TupleRecorder<Tuple_ extends Tuple>(List<Tuple_> recordedTupleList,
        UnaryOperator<Tuple_> mapper,
        IdentityHashMap<Tuple_, Tuple_> inputTupleToOutputTuple) {
    public void recordTuple(Tuple_ tuple) {
        var outTuple = inputTupleToOutputTuple.computeIfAbsent(tuple, mapper);
        if (tuple.getIndictmentSource() != IndictmentSource.DISABLED) {
            // Precompute uses an independent node network, so we need to aggregate its supports to not interfere with the
            // outer node network support
            var aggregateIndictmentSource = IndictmentSource.getPrecomputeAggregation(outTuple);
            tuple.getIndictmentSource()
                    .visitAllSources(source -> aggregateIndictmentSource.sourceList().add(IndictmentSource.of(source)));
        }
        recordedTupleList.add(outTuple);
    }
}
