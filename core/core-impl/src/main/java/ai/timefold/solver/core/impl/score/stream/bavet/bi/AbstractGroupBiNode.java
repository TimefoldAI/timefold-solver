package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import java.util.function.Function;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractGroupNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;

abstract class AbstractGroupBiNode<OldA, OldB, OutTuple_ extends AbstractTuple, GroupKey_, ResultContainer_, Result_>
        extends AbstractGroupNode<BiTuple<OldA, OldB>, OutTuple_, GroupKey_, ResultContainer_, Result_> {

    private final TriFunction<ResultContainer_, OldA, OldB, Runnable> accumulator;

    protected AbstractGroupBiNode(int groupStoreIndex, int undoStoreIndex,
            Function<BiTuple<OldA, OldB>, GroupKey_> groupKeyFunction,
            BiConstraintCollector<OldA, OldB, ResultContainer_, Result_> collector,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex,
                groupKeyFunction,
                collector == null ? null : collector.supplier(),
                collector == null ? null : collector.finisher(),
                nextNodesTupleLifecycle, environmentMode);
        accumulator = collector == null ? null : collector.accumulator();
    }

    protected AbstractGroupBiNode(int groupStoreIndex,
            Function<BiTuple<OldA, OldB>, GroupKey_> groupKeyFunction, TupleLifecycle<OutTuple_> nextNodesTupleLifecycle,
            EnvironmentMode environmentMode) {
        super(groupStoreIndex,
                groupKeyFunction, nextNodesTupleLifecycle, environmentMode);
        accumulator = null;
    }

    @Override
    protected final Runnable accumulate(ResultContainer_ resultContainer, BiTuple<OldA, OldB> tuple) {
        return accumulator.apply(resultContainer, tuple.factA, tuple.factB);
    }

}
