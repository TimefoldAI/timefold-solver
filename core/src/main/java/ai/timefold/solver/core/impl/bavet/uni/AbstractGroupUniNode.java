package ai.timefold.solver.core.impl.bavet.uni;

import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.AbstractGroupNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.jspecify.annotations.Nullable;

abstract class AbstractGroupUniNode<OldA, OutTuple_ extends Tuple, GroupKey_, ResultContainer_, Result_>
        extends AbstractGroupNode<UniTuple<OldA>, OutTuple_, GroupKey_, ResultContainer_, Result_> {

    private final int groupAccumulatorIndex;
    private final @Nullable BiFunction<ResultContainer_, OldA, Runnable> accumulator;
    private final @Nullable UniConstraintCollectorAccumulator<ResultContainer_, OldA> incrementalAccumulator;
    private final boolean useIncrementalAccumulator;

    protected AbstractGroupUniNode(int groupStoreIndex, int groupAccumulatorIndex,
            Function<UniTuple<OldA>, GroupKey_> groupKeyFunction,
            UniConstraintCollector<OldA, ResultContainer_, Result_> collector,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, EnvironmentMode environmentMode) {
        super(groupStoreIndex, groupKeyFunction,
                collector == null ? null : collector.supplier(),
                collector == null ? null : collector.finisher(),
                nextNodesTupleLifecycle, environmentMode);
        var hasCollector = collector != null;
        this.groupAccumulatorIndex = hasCollector ? groupAccumulatorIndex : -1;
        accumulator = hasCollector ? (collector.isIncremental() ? null : collector.accumulator()) : null;
        incrementalAccumulator = hasCollector ? (collector.isIncremental() ? collector.incrementalAccumulator() : null) : null;
        useIncrementalAccumulator = hasCollector && incrementalAccumulator != null;
    }

    protected AbstractGroupUniNode(int groupStoreIndex, Function<UniTuple<OldA>, GroupKey_> groupKeyFunction,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, EnvironmentMode environmentMode) {
        super(groupStoreIndex, groupKeyFunction, nextNodesTupleLifecycle, environmentMode);
        groupAccumulatorIndex = -1;
        accumulator = null;
        incrementalAccumulator = null;
        useIncrementalAccumulator = false;
    }

    @Override
    protected boolean groupInsert(ResultContainer_ resultContainer, UniTuple<OldA> tuple) {
        if (useIncrementalAccumulator) {
            var groupContents = incrementalAccumulator.intoGroup(resultContainer);
            tuple.setStore(groupAccumulatorIndex, groupContents);
            return groupContents.add(tuple.getA());
        } else {
            tuple.setStore(groupAccumulatorIndex, accumulator.apply(resultContainer, tuple.getA()));
            return true;
        }
    }

    @Override
    protected boolean groupUpdate(ResultContainer_ resultContainer, UniTuple<OldA> tuple) {
        if (useIncrementalAccumulator) {
            UniConstraintCollectorAccumulatedValue<OldA> groupContents = tuple.getStore(groupAccumulatorIndex);
            return groupContents.update(tuple.getA());
        } else {
            return super.groupUpdate(resultContainer, tuple);
        }
    }

    @Override
    protected boolean groupRetract(UniTuple<OldA> tuple) {
        if (useIncrementalAccumulator) {
            UniConstraintCollectorAccumulatedValue<OldA> groupContents = tuple.removeStore(groupAccumulatorIndex);
            return groupContents.remove();
        } else {
            Runnable undo = tuple.removeStore(groupAccumulatorIndex);
            undo.run();
            return true;
        }
    }

}
