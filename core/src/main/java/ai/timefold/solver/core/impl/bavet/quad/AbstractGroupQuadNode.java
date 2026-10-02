package ai.timefold.solver.core.impl.bavet.quad;

import java.util.function.Function;
import java.util.function.IntSupplier;

import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorValueHandle;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.AbstractGroupNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.collector.quad.QuadCollectorUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

abstract class AbstractGroupQuadNode<OldA, OldB, OldC, OldD, OutTuple_ extends Tuple, GroupKey_, ResultContainer_, Result_>
        extends AbstractGroupNode<QuadTuple<OldA, OldB, OldC, OldD>, OutTuple_, GroupKey_, ResultContainer_, Result_> {

    private final int groupAccumulatorIndex;
    private final @Nullable QuadConstraintCollectorAccumulator<ResultContainer_, OldA, OldB, OldC, OldD> incrementalAccumulator;

    protected AbstractGroupQuadNode(IntSupplier storeIndexReserver,
            Function<QuadTuple<OldA, OldB, OldC, OldD>, GroupKey_> groupKeyFunction,
            @NonNull QuadConstraintCollector<OldA, OldB, OldC, OldD, ResultContainer_, Result_> collector,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, EnvironmentMode environmentMode) {
        super(storeIndexReserver, groupKeyFunction, collector.supplier(), collector.finisher(), nextNodesTupleLifecycle,
                environmentMode);
        this.groupAccumulatorIndex = storeIndexReserver.getAsInt();
        this.incrementalAccumulator = QuadCollectorUtils.toIncremental(collector.accumulator());
    }

    protected AbstractGroupQuadNode(IntSupplier storeIndexReserver,
            Function<QuadTuple<OldA, OldB, OldC, OldD>, GroupKey_> groupKeyFunction,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, EnvironmentMode environmentMode) {
        super(storeIndexReserver, groupKeyFunction, nextNodesTupleLifecycle, environmentMode);
        this.groupAccumulatorIndex = -1;
        this.incrementalAccumulator = null;
    }

    @Override
    protected void groupInsert(ResultContainer_ resultContainer, QuadTuple<OldA, OldB, OldC, OldD> tuple) {
        var groupElement = incrementalAccumulator.intoGroup(resultContainer);
        tuple.setStore(groupAccumulatorIndex, groupElement);
        groupElement.add(tuple.getA(), tuple.getB(), tuple.getC(), tuple.getD());
    }

    @Override
    protected void groupUpdate(ResultContainer_ resultContainer, QuadTuple<OldA, OldB, OldC, OldD> tuple) {
        QuadConstraintCollectorValueHandle<OldA, OldB, OldC, OldD> groupElement = tuple.getStore(groupAccumulatorIndex);
        groupElement.replaceWith(tuple.getA(), tuple.getB(), tuple.getC(), tuple.getD());
    }

    @Override
    protected void groupRetract(QuadTuple<OldA, OldB, OldC, OldD> tuple) {
        QuadConstraintCollectorValueHandle<OldA, OldB, OldC, OldD> groupElement = tuple.removeStore(groupAccumulatorIndex);
        groupElement.remove();
    }

}
