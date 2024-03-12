package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import java.util.function.Function;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractGroupNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;

abstract class AbstractGroupTriNode<OldA, OldB, OldC, OutTuple_ extends AbstractTuple, GroupKey_, ResultContainer_, Result_>
        extends AbstractGroupNode<TriTuple<OldA, OldB, OldC>, OutTuple_, GroupKey_, ResultContainer_, Result_> {

    private final QuadFunction<ResultContainer_, OldA, OldB, OldC, Runnable> accumulator;

    protected AbstractGroupTriNode(int groupStoreIndex, int undoStoreIndex,
            Function<TriTuple<OldA, OldB, OldC>, GroupKey_> groupKeyFunction,
            TriConstraintCollector<OldA, OldB, OldC, ResultContainer_, Result_> collector,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex, groupKeyFunction,
                collector == null ? null : collector.supplier(),
                collector == null ? null : collector.finisher(),
                nextNodesTupleLifecycle, environmentMode);
        accumulator = collector == null ? null : collector.accumulator();
    }

    protected AbstractGroupTriNode(int groupStoreIndex,
            Function<TriTuple<OldA, OldB, OldC>, GroupKey_> groupKeyFunction,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, EnvironmentMode environmentMode) {
        super(groupStoreIndex,
                groupKeyFunction, nextNodesTupleLifecycle, environmentMode);
        accumulator = null;
    }

    @Override
    protected final Runnable accumulate(ResultContainer_ resultContainer, TriTuple<OldA, OldB, OldC> tuple) {
        return accumulator.apply(resultContainer, tuple.factA, tuple.factB, tuple.factC);
    }

}
