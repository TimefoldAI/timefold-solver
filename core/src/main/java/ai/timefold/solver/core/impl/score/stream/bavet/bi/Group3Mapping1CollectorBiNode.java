package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.util.Triple;

final class Group3Mapping1CollectorBiNode<OldA, OldB, A, B, C, D, ResultContainer_>
        extends
        AbstractGroupBiNode<OldA, OldB, QuadTuple<A, B, C, D>, Triple<A, B, C>, ResultContainer_, D> {

    private final int outputStoreSize;

    public Group3Mapping1CollectorBiNode(BiFunction<OldA, OldB, A> groupKeyMappingA,
            BiFunction<OldA, OldB, B> groupKeyMappingB, BiFunction<OldA, OldB, C> groupKeyMappingC,
            int groupStoreIndex, int undoStoreIndex,
            BiConstraintCollector<OldA, OldB, ResultContainer_, D> collector,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle, int outputStoreSize,
            EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex,
                tuple -> Group3Mapping0CollectorBiNode.createGroupKey(groupKeyMappingA, groupKeyMappingB, groupKeyMappingC,
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
