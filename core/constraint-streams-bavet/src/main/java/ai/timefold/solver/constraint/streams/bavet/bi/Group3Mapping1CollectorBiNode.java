package ai.timefold.solver.constraint.streams.bavet.bi;

import static ai.timefold.solver.constraint.streams.bavet.bi.Group3Mapping0CollectorBiNode.createGroupKey;

import java.util.function.BiFunction;

import ai.timefold.solver.constraint.streams.bavet.common.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.quad.QuadTuple;
import ai.timefold.solver.constraint.streams.bavet.quad.QuadTupleImpl;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.util.Triple;

final class Group3Mapping1CollectorBiNode<OldA, OldB, A, B, C, D, ResultContainer_>
        extends
        AbstractGroupBiNode<OldA, OldB, QuadTuple<A, B, C, D>, QuadTupleImpl<A, B, C, D>, Triple<A, B, C>, ResultContainer_, D> {

    private final int outputStoreSize;

    public Group3Mapping1CollectorBiNode(BiFunction<OldA, OldB, A> groupKeyMappingA,
            BiFunction<OldA, OldB, B> groupKeyMappingB, BiFunction<OldA, OldB, C> groupKeyMappingC,
            int groupStoreIndex, int undoStoreIndex, BiConstraintCollector<OldA, OldB, ResultContainer_, D> collector,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle, int outputStoreSize,
            EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex,
                tuple -> createGroupKey(groupKeyMappingA, groupKeyMappingB, groupKeyMappingC, tuple), collector,
                nextNodesTupleLifecycle, environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected QuadTupleImpl<A, B, C, D> createOutTuple(Triple<A, B, C> groupKey) {
        return new QuadTupleImpl<>(groupKey.getA(), groupKey.getB(), groupKey.getC(), null, outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(QuadTupleImpl<A, B, C, D> outTuple, D d) {
        outTuple.factD = d;
    }

}
