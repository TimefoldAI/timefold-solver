package ai.timefold.solver.core.impl.bavet.bi;

import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorValueHandle;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.AbstractGroupNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.collector.bi.BiCollectorUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

abstract class AbstractGroupBiNode<OldA, OldB, OutTuple_ extends Tuple, GroupKey_, ResultContainer_, Result_>
        extends AbstractGroupNode<BiTuple<OldA, OldB>, OutTuple_, GroupKey_, ResultContainer_, Result_> {

    private final int groupAccumulatorIndex;
    private final @Nullable BiConstraintCollectorAccumulator<ResultContainer_, OldA, OldB> incrementalAccumulator;

    protected AbstractGroupBiNode(int groupStoreIndex, int groupAccumulatorIndex,
            Function<BiTuple<OldA, OldB>, GroupKey_> groupKeyFunction,
            @NonNull BiConstraintCollector<OldA, OldB, ResultContainer_, Result_> collector,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, EnvironmentMode environmentMode) {
        super(groupStoreIndex, groupKeyFunction, collector.supplier(), collector.finisher(), nextNodesTupleLifecycle,
                environmentMode);
        this.groupAccumulatorIndex = groupAccumulatorIndex;
        this.incrementalAccumulator = collector.isIncremental() ? collector.incrementalAccumulator()
                : BiCollectorUtils.toIncremental(collector.accumulator());
    }

    protected AbstractGroupBiNode(int groupStoreIndex, Function<BiTuple<OldA, OldB>, GroupKey_> groupKeyFunction,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, EnvironmentMode environmentMode) {
        super(groupStoreIndex, groupKeyFunction, nextNodesTupleLifecycle, environmentMode);
        this.groupAccumulatorIndex = -1;
        this.incrementalAccumulator = null;
    }

    @Override
    protected void groupInsert(ResultContainer_ resultContainer, BiTuple<OldA, OldB> tuple) {
        var groupElement = incrementalAccumulator.intoGroup(resultContainer);
        tuple.setStore(groupAccumulatorIndex, groupElement);
        groupElement.add(tuple.getA(), tuple.getB());
    }

    @Override
    protected void groupUpdate(ResultContainer_ resultContainer, BiTuple<OldA, OldB> tuple) {
        BiConstraintCollectorValueHandle<OldA, OldB> groupElement = tuple.getStore(groupAccumulatorIndex);
        groupElement.replaceWith(tuple.getA(), tuple.getB());
    }

    @Override
    protected void groupRetract(BiTuple<OldA, OldB> tuple) {
        BiConstraintCollectorValueHandle<OldA, OldB> groupElement = tuple.removeStore(groupAccumulatorIndex);
        groupElement.remove();
    }
}
