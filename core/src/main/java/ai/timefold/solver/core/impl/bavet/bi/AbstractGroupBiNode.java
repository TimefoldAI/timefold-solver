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

    private final int undoStoreIndex;
    private final @Nullable TriFunction<ResultContainer_, OldA, OldB, Runnable> accumulator;
    private final @Nullable BiConstraintCollectorAccumulator<ResultContainer_, OldA, OldB> incrementalAccumulator;
    private final boolean useIncrementalAccumulator;

    protected AbstractGroupBiNode(int groupStoreIndex, int undoStoreIndex,
            Function<BiTuple<OldA, OldB>, GroupKey_> groupKeyFunction,
            BiConstraintCollector<OldA, OldB, ResultContainer_, Result_> collector,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, EnvironmentMode environmentMode) {
        super(groupStoreIndex, groupKeyFunction,
                collector == null ? null : collector.supplier(),
                collector == null ? null : collector.finisher(),
                nextNodesTupleLifecycle, environmentMode);
        var hasCollector = collector != null;
        this.undoStoreIndex = hasCollector ? undoStoreIndex : -1;
        accumulator = hasCollector ? (collector.isIncremental() ? null : collector.accumulator()) : null;
        incrementalAccumulator = hasCollector ? (collector.isIncremental() ? collector.incrementalAccumulator() : null) : null;
        useIncrementalAccumulator = hasCollector && incrementalAccumulator != null;
    }

    protected AbstractGroupBiNode(int groupStoreIndex, Function<BiTuple<OldA, OldB>, GroupKey_> groupKeyFunction,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, EnvironmentMode environmentMode) {
        super(groupStoreIndex, groupKeyFunction, nextNodesTupleLifecycle, environmentMode);
        undoStoreIndex = -1;
        accumulator = null;
        incrementalAccumulator = null;
        useIncrementalAccumulator = false;
    }

    @Override
    protected void groupInsert(ResultContainer_ resultContainer, BiTuple<OldA, OldB> tuple) {
        if (useIncrementalAccumulator) {
            var undoAccumulator = incrementalAccumulator.startGroup(resultContainer);
            undoAccumulator.add(tuple.getA(), tuple.getB());
            tuple.setStore(undoStoreIndex, undoAccumulator);
        } else {
            var undoAccumulator = accumulator.apply(resultContainer, tuple.getA(), tuple.getB());
            tuple.setStore(undoStoreIndex, undoAccumulator);
        }
    }

    @Override
    protected boolean groupUpdate(ResultContainer_ resultContainer, BiTuple<OldA, OldB> tuple) {
        if (useIncrementalAccumulator) {
            BiConstraintCollectorAccumulatedValue<OldA, OldB> undoAccumulator = tuple.getStore(undoStoreIndex);
            return undoAccumulator.update(tuple.getA(), tuple.getB());
        } else {
            return super.groupUpdate(resultContainer, tuple);
        }
    }

    @Override
    protected void groupRetract(ResultContainer_ resultContainer, BiTuple<OldA, OldB> tuple) {
        if (useIncrementalAccumulator) {
            BiConstraintCollectorAccumulatedValue<OldA, OldB> undoAccumulator = tuple.removeStore(undoStoreIndex);
            undoAccumulator.remove();
        } else {
            Runnable undoAccumulator = tuple.removeStore(undoStoreIndex);
            undoAccumulator.run();
        }
    }
}
