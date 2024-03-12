package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.util.Triple;

final class Group3Mapping1CollectorQuadNode<OldA, OldB, OldC, OldD, A, B, C, D, ResultContainer_>
        extends
        AbstractGroupQuadNode<OldA, OldB, OldC, OldD, QuadTuple<A, B, C, D>, Triple<A, B, C>, ResultContainer_, D> {

    private final int outputStoreSize;

    public Group3Mapping1CollectorQuadNode(QuadFunction<OldA, OldB, OldC, OldD, A> groupKeyMappingA,
            QuadFunction<OldA, OldB, OldC, OldD, B> groupKeyMappingB, QuadFunction<OldA, OldB, OldC, OldD, C> groupKeyMappingC,
            int groupStoreIndex, int undoStoreIndex,
            QuadConstraintCollector<OldA, OldB, OldC, OldD, ResultContainer_, D> collector,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle, int outputStoreSize,
            EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex,
                tuple -> Group3Mapping0CollectorQuadNode.createGroupKey(groupKeyMappingA, groupKeyMappingB, groupKeyMappingC,
                        tuple),
                collector,
                nextNodesTupleLifecycle, environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected QuadTuple<A, B, C, D> createOutTuple(Triple<A, B, C> groupKey) {
        return new QuadTuple<>(groupKey.a(), groupKey.b(), groupKey.c(), null, outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(QuadTuple<A, B, C, D> outTuple, D d) {
        outTuple.factD = d;
    }

}
