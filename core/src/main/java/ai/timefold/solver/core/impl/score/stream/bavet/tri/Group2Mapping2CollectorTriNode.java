package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.util.Pair;

final class Group2Mapping2CollectorTriNode<OldA, OldB, OldC, A, B, C, D, ResultContainerC_, ResultContainerD_>
        extends
        AbstractGroupTriNode<OldA, OldB, OldC, QuadTuple<A, B, C, D>, Pair<A, B>, Object, Pair<C, D>> {

    private final int outputStoreSize;

    public Group2Mapping2CollectorTriNode(TriFunction<OldA, OldB, OldC, A> groupKeyMappingA,
            TriFunction<OldA, OldB, OldC, B> groupKeyMappingB,
            int groupStoreIndex, int undoStoreIndex,
            TriConstraintCollector<OldA, OldB, OldC, ResultContainerC_, C> collectorC,
            TriConstraintCollector<OldA, OldB, OldC, ResultContainerD_, D> collectorD,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle, int outputStoreSize,
            EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex,
                tuple -> Group2Mapping0CollectorTriNode.createGroupKey(groupKeyMappingA, groupKeyMappingB, tuple),
                Group0Mapping2CollectorTriNode.mergeCollectors(collectorC, collectorD), nextNodesTupleLifecycle,
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
