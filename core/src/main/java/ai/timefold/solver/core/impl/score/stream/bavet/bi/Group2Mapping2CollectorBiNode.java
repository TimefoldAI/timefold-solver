package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.util.Pair;

final class Group2Mapping2CollectorBiNode<OldA, OldB, A, B, C, D, ResultContainerC_, ResultContainerD_>
        extends
        AbstractGroupBiNode<OldA, OldB, QuadTuple<A, B, C, D>, Pair<A, B>, Object, Pair<C, D>> {

    private final int outputStoreSize;

    public Group2Mapping2CollectorBiNode(BiFunction<OldA, OldB, A> groupKeyMappingA, BiFunction<OldA, OldB, B> groupKeyMappingB,
            int groupStoreIndex, int undoStoreIndex,
            BiConstraintCollector<OldA, OldB, ResultContainerC_, C> collectorC,
            BiConstraintCollector<OldA, OldB, ResultContainerD_, D> collectorD,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle, int outputStoreSize,
            EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex,
                tuple -> Group2Mapping0CollectorBiNode.createGroupKey(groupKeyMappingA, groupKeyMappingB, tuple),
                Group0Mapping2CollectorBiNode.mergeCollectors(collectorC, collectorD), nextNodesTupleLifecycle,
                environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected QuadTuple<A, B, C, D> createOutTuple(Pair<A, B> groupKey) {
        return new QuadTuple<>(groupKey.key(), groupKey.value(), null, null, outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(QuadTuple<A, B, C, D> outTuple, Pair<C, D> result) {
        outTuple.factC = result.key();
        outTuple.factD = result.value();
    }

}
