package ai.timefold.solver.core.impl.bavet.uni;

import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorValueHandle;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.AbstractGroupNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.score.stream.collector.uni.UniCollectorUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

abstract class AbstractGroupUniNode<OldA, OutTuple_ extends Tuple, GroupKey_, ResultContainer_, Result_>
        extends AbstractGroupNode<UniTuple<OldA>, OutTuple_, GroupKey_, ResultContainer_, Result_> {

    private final int groupAccumulatorIndex;
    private final @Nullable UniConstraintCollectorAccumulator<ResultContainer_, OldA> incrementalAccumulator;

    protected AbstractGroupUniNode(int groupStoreIndex, int groupAccumulatorIndex,
            Function<UniTuple<OldA>, GroupKey_> groupKeyFunction,
            @NonNull UniConstraintCollector<OldA, ResultContainer_, Result_> collector,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, EnvironmentMode environmentMode) {
        super(groupStoreIndex, groupKeyFunction, collector.supplier(), collector.finisher(), nextNodesTupleLifecycle,
                environmentMode);
        this.groupAccumulatorIndex = groupAccumulatorIndex;
        this.incrementalAccumulator = UniCollectorUtils.toIncremental(collector.accumulator());
    }

    protected AbstractGroupUniNode(int groupStoreIndex, Function<UniTuple<OldA>, GroupKey_> groupKeyFunction,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, EnvironmentMode environmentMode) {
        super(groupStoreIndex, groupKeyFunction, nextNodesTupleLifecycle, environmentMode);
        this.groupAccumulatorIndex = -1;
        this.incrementalAccumulator = null;
    }

    @Override
    protected void groupInsert(ResultContainer_ resultContainer, UniTuple<OldA> tuple) {
        var groupElement = incrementalAccumulator.intoGroup(resultContainer);
        tuple.setStore(groupAccumulatorIndex, groupElement);
        groupElement.add(tuple.getA());
    }

    @Override
    protected void groupUpdate(ResultContainer_ resultContainer, UniTuple<OldA> tuple) {
        UniConstraintCollectorValueHandle<OldA> groupElement = tuple.getStore(groupAccumulatorIndex);
        groupElement.replaceWith(tuple.getA());
    }

    @Override
    protected void groupRetract(UniTuple<OldA> tuple) {
        UniConstraintCollectorValueHandle<OldA> groupElement = tuple.removeStore(groupAccumulatorIndex);
        groupElement.remove();
    }

}
