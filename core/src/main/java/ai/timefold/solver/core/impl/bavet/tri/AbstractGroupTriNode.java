package ai.timefold.solver.core.impl.bavet.tri;

import java.util.function.Function;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorAccumulatedComponent;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorIncrementalAccumulator;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.AbstractGroupNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

import org.jspecify.annotations.Nullable;

abstract class AbstractGroupTriNode<OldA, OldB, OldC, OutTuple_ extends Tuple, GroupKey_, ResultContainer_, Result_>
        extends AbstractGroupNode<TriTuple<OldA, OldB, OldC>, OutTuple_, GroupKey_, ResultContainer_, Result_> {

    private final int undoStoreIndex;
    private final @Nullable QuadFunction<ResultContainer_, OldA, OldB, OldC, Runnable> accumulator;
    private final @Nullable TriConstraintCollectorIncrementalAccumulator<ResultContainer_, OldA, OldB, OldC> incrementalAccumulator;
    private final boolean useIncrementalAccumulator;

    protected AbstractGroupTriNode(int groupStoreIndex, int undoStoreIndex,
            Function<TriTuple<OldA, OldB, OldC>, GroupKey_> groupKeyFunction,
            TriConstraintCollector<OldA, OldB, OldC, ResultContainer_, Result_> collector,
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

    protected AbstractGroupTriNode(int groupStoreIndex, Function<TriTuple<OldA, OldB, OldC>, GroupKey_> groupKeyFunction,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, EnvironmentMode environmentMode) {
        super(groupStoreIndex, groupKeyFunction, nextNodesTupleLifecycle, environmentMode);
        undoStoreIndex = -1;
        accumulator = null;
        incrementalAccumulator = null;
        useIncrementalAccumulator = false;
    }

    @Override
    protected void groupInsert(ResultContainer_ resultContainer, TriTuple<OldA, OldB, OldC> tuple) {
        if (useIncrementalAccumulator) {
            var undoAccumulator = incrementalAccumulator.accumulate(resultContainer, tuple.getA(), tuple.getB(), tuple.getC());
            tuple.setStore(undoStoreIndex, undoAccumulator);
        } else {
            var undoAccumulator = accumulator.apply(resultContainer, tuple.getA(), tuple.getB(), tuple.getC());
            tuple.setStore(undoStoreIndex, undoAccumulator);
        }
    }

    @Override
    protected boolean groupUpdate(ResultContainer_ resultContainer, TriTuple<OldA, OldB, OldC> tuple) {
        if (useIncrementalAccumulator) {
            TriConstraintCollectorAccumulatedComponent<ResultContainer_, OldA, OldB, OldC> undoAccumulator =
                    tuple.getStore(undoStoreIndex);
            return undoAccumulator.update(resultContainer, tuple.getA(), tuple.getB(), tuple.getC());
        } else {
            return super.groupUpdate(resultContainer, tuple);
        }
    }

    @Override
    protected void groupRetract(ResultContainer_ resultContainer, TriTuple<OldA, OldB, OldC> tuple) {
        if (useIncrementalAccumulator) {
            TriConstraintCollectorAccumulatedComponent<OldC, ResultContainer_, OldA, OldB> undoAccumulator =
                    tuple.removeStore(undoStoreIndex);
            undoAccumulator.undo();
        } else {
            Runnable undoAccumulator = tuple.removeStore(undoStoreIndex);
            undoAccumulator.run();
        }
    }

}
