package ai.timefold.solver.core.impl.bavet.quad;

import java.util.function.Function;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorAccumulator;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.AbstractGroupNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

import org.jspecify.annotations.Nullable;

abstract class AbstractGroupQuadNode<OldA, OldB, OldC, OldD, OutTuple_ extends Tuple, GroupKey_, ResultContainer_, Result_>
        extends AbstractGroupNode<QuadTuple<OldA, OldB, OldC, OldD>, OutTuple_, GroupKey_, ResultContainer_, Result_> {

    private final int undoStoreIndex;
    private final @Nullable PentaFunction<ResultContainer_, OldA, OldB, OldC, OldD, Runnable> accumulator;
    private final @Nullable QuadConstraintCollectorAccumulator<ResultContainer_, OldA, OldB, OldC, OldD> incrementalAccumulator;
    private final boolean useIncrementalAccumulator;

    protected AbstractGroupQuadNode(int groupStoreIndex, int undoStoreIndex,
            Function<QuadTuple<OldA, OldB, OldC, OldD>, GroupKey_> groupKeyFunction,
            QuadConstraintCollector<OldA, OldB, OldC, OldD, ResultContainer_, Result_> collector,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, EnvironmentMode environmentMode) {
        super(groupStoreIndex,
                groupKeyFunction,
                collector == null ? null : collector.supplier(),
                collector == null ? null : collector.finisher(),
                nextNodesTupleLifecycle, environmentMode);
        var hasCollector = collector != null;
        this.undoStoreIndex = hasCollector ? undoStoreIndex : -1;
        accumulator = hasCollector ? (collector.isIncremental() ? null : collector.accumulator()) : null;
        incrementalAccumulator = hasCollector ? (collector.isIncremental() ? collector.incrementalAccumulator() : null) : null;
        useIncrementalAccumulator = hasCollector && incrementalAccumulator != null;
    }

    protected AbstractGroupQuadNode(int groupStoreIndex,
            Function<QuadTuple<OldA, OldB, OldC, OldD>, GroupKey_> groupKeyFunction,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, EnvironmentMode environmentMode) {
        super(groupStoreIndex, groupKeyFunction, nextNodesTupleLifecycle, environmentMode);
        undoStoreIndex = -1;
        accumulator = null;
        incrementalAccumulator = null;
        useIncrementalAccumulator = false;
    }

    @Override
    protected void groupInsert(ResultContainer_ resultContainer, QuadTuple<OldA, OldB, OldC, OldD> tuple) {
        if (useIncrementalAccumulator) {
            var undoAccumulator = incrementalAccumulator.startGroup(resultContainer);
            undoAccumulator.add(tuple.getA(), tuple.getB(), tuple.getC(), tuple.getD());
            tuple.setStore(undoStoreIndex, undoAccumulator);
        } else {
            var undoAccumulator = accumulator.apply(resultContainer, tuple.getA(), tuple.getB(), tuple.getC(), tuple.getD());
            tuple.setStore(undoStoreIndex, undoAccumulator);
        }
    }

    @Override
    protected boolean groupUpdate(ResultContainer_ resultContainer, QuadTuple<OldA, OldB, OldC, OldD> tuple) {
        if (useIncrementalAccumulator) {
            QuadConstraintCollectorAccumulatedValue<OldA, OldB, OldC, OldD> undoAccumulator = tuple.getStore(undoStoreIndex);
            return undoAccumulator.update(tuple.getA(), tuple.getB(), tuple.getC(), tuple.getD());
        } else {
            return super.groupUpdate(resultContainer, tuple);
        }
    }

    @Override
    protected void groupRetract(ResultContainer_ resultContainer, QuadTuple<OldA, OldB, OldC, OldD> tuple) {
        if (useIncrementalAccumulator) {
            QuadConstraintCollectorAccumulatedValue<OldA, OldB, OldC, OldD> undoAccumulator = tuple.removeStore(undoStoreIndex);
            undoAccumulator.remove();
        } else {
            Runnable undoAccumulator = tuple.removeStore(undoStoreIndex);
            undoAccumulator.run();
        }
    }

}
