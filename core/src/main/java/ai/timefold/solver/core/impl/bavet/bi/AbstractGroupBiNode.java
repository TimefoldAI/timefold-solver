package ai.timefold.solver.core.impl.bavet.bi;

import java.util.function.Function;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulator;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.AbstractGroupNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

import org.jspecify.annotations.Nullable;

abstract class AbstractGroupBiNode<OldA, OldB, OutTuple_ extends Tuple, GroupKey_, ResultContainer_, Result_>
        extends AbstractGroupNode<BiTuple<OldA, OldB>, OutTuple_, GroupKey_, ResultContainer_, Result_> {

    private final int groupAccumulatorIndex;
    private final @Nullable TriFunction<ResultContainer_, OldA, OldB, Runnable> accumulator;
    private final @Nullable BiConstraintCollectorAccumulator<ResultContainer_, OldA, OldB> incrementalAccumulator;
    private final boolean useIncrementalAccumulator;

    protected AbstractGroupBiNode(int groupStoreIndex, int groupAccumulatorIndex,
            Function<BiTuple<OldA, OldB>, GroupKey_> groupKeyFunction,
            BiConstraintCollector<OldA, OldB, ResultContainer_, Result_> collector,
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

    protected AbstractGroupBiNode(int groupStoreIndex, Function<BiTuple<OldA, OldB>, GroupKey_> groupKeyFunction,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, EnvironmentMode environmentMode) {
        super(groupStoreIndex, groupKeyFunction, nextNodesTupleLifecycle, environmentMode);
        groupAccumulatorIndex = -1;
        accumulator = null;
        incrementalAccumulator = null;
        useIncrementalAccumulator = false;
    }

    @Override
    protected boolean groupInsert(ResultContainer_ resultContainer, BiTuple<OldA, OldB> tuple) {
        if (useIncrementalAccumulator) {
            var groupContents = incrementalAccumulator.startGroup(resultContainer);
            tuple.setStore(groupAccumulatorIndex, groupContents);
            return groupContents.add(tuple.getA(), tuple.getB());
        } else {
            tuple.setStore(groupAccumulatorIndex, accumulator.apply(resultContainer, tuple.getA(), tuple.getB()));
            return true;
        }
    }

    @Override
    protected boolean groupUpdate(ResultContainer_ resultContainer, BiTuple<OldA, OldB> tuple) {
        if (useIncrementalAccumulator) {
            BiConstraintCollectorAccumulatedValue<OldA, OldB> groupContents = tuple.getStore(groupAccumulatorIndex);
            return groupContents.update(tuple.getA(), tuple.getB());
        } else {
            return super.groupUpdate(resultContainer, tuple);
        }
    }

    @Override
    protected boolean groupRetract(BiTuple<OldA, OldB> tuple) {
        if (useIncrementalAccumulator) {
            BiConstraintCollectorAccumulatedValue<OldA, OldB> groupContents = tuple.removeStore(groupAccumulatorIndex);
            return groupContents.remove();
        } else {
            Runnable undo = tuple.removeStore(groupAccumulatorIndex);
            undo.run();
            return true;
        }
    }
}
