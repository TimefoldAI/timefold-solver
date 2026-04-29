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

    private final int groupAccumulatorIndex;
    private final @Nullable PentaFunction<ResultContainer_, OldA, OldB, OldC, OldD, Runnable> accumulator;
    private final @Nullable QuadConstraintCollectorAccumulator<ResultContainer_, OldA, OldB, OldC, OldD> incrementalAccumulator;
    private final boolean useIncrementalAccumulator;

    protected AbstractGroupQuadNode(int groupStoreIndex, int groupAccumulatorIndex,
            Function<QuadTuple<OldA, OldB, OldC, OldD>, GroupKey_> groupKeyFunction,
            QuadConstraintCollector<OldA, OldB, OldC, OldD, ResultContainer_, Result_> collector,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, EnvironmentMode environmentMode) {
        super(groupStoreIndex,
                groupKeyFunction,
                collector == null ? null : collector.supplier(),
                collector == null ? null : collector.finisher(),
                nextNodesTupleLifecycle, environmentMode);
        var hasCollector = collector != null;
        this.groupAccumulatorIndex = hasCollector ? groupAccumulatorIndex : -1;
        accumulator = hasCollector ? (collector.isIncremental() ? null : collector.accumulator()) : null;
        incrementalAccumulator = hasCollector ? (collector.isIncremental() ? collector.incrementalAccumulator() : null) : null;
        useIncrementalAccumulator = hasCollector && incrementalAccumulator != null;
    }

    protected AbstractGroupQuadNode(int groupStoreIndex,
            Function<QuadTuple<OldA, OldB, OldC, OldD>, GroupKey_> groupKeyFunction,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, EnvironmentMode environmentMode) {
        super(groupStoreIndex, groupKeyFunction, nextNodesTupleLifecycle, environmentMode);
        groupAccumulatorIndex = -1;
        accumulator = null;
        incrementalAccumulator = null;
        useIncrementalAccumulator = false;
    }

    @Override
    protected boolean groupInsert(ResultContainer_ resultContainer, QuadTuple<OldA, OldB, OldC, OldD> tuple) {
        if (useIncrementalAccumulator) {
            var groupContents = incrementalAccumulator.startGroup(resultContainer);
            tuple.setStore(groupAccumulatorIndex, groupContents);
            return groupContents.add(tuple.getA(), tuple.getB(), tuple.getC(), tuple.getD());
        } else {
            tuple.setStore(groupAccumulatorIndex,
                    accumulator.apply(resultContainer, tuple.getA(), tuple.getB(), tuple.getC(), tuple.getD()));
            return true;
        }
    }

    @Override
    protected boolean groupUpdate(ResultContainer_ resultContainer, QuadTuple<OldA, OldB, OldC, OldD> tuple) {
        if (useIncrementalAccumulator) {
            QuadConstraintCollectorAccumulatedValue<OldA, OldB, OldC, OldD> groupContents =
                    tuple.getStore(groupAccumulatorIndex);
            return groupContents.update(tuple.getA(), tuple.getB(), tuple.getC(), tuple.getD());
        } else {
            return super.groupUpdate(resultContainer, tuple);
        }
    }

    @Override
    protected boolean groupRetract(QuadTuple<OldA, OldB, OldC, OldD> tuple) {
        if (useIncrementalAccumulator) {
            QuadConstraintCollectorAccumulatedValue<OldA, OldB, OldC, OldD> groupContents =
                    tuple.removeStore(groupAccumulatorIndex);
            return groupContents.remove();
        } else {
            Runnable undo = tuple.removeStore(groupAccumulatorIndex);
            undo.run();
            return true;
        }
    }

}
