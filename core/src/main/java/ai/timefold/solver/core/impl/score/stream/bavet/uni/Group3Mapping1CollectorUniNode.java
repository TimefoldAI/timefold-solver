package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import static ai.timefold.solver.core.impl.score.stream.bavet.uni.Group3Mapping0CollectorUniNode.createGroupKey;

import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.util.Triple;

final class Group3Mapping1CollectorUniNode<OldA, A, B, C, D, ResultContainer_>
        extends
        AbstractGroupUniNode<OldA, QuadTuple<A, B, C, D>, Triple<A, B, C>, ResultContainer_, D> {

    private final int outputStoreSize;

    public Group3Mapping1CollectorUniNode(Function<OldA, A> groupKeyMappingA, Function<OldA, B> groupKeyMappingB,
            Function<OldA, C> groupKeyMappingC,
            int groupStoreIndex, int undoStoreIndex,
            UniConstraintCollector<OldA, ResultContainer_, D> collector,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle, int outputStoreSize,
            EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex,
                tuple -> createGroupKey(groupKeyMappingA, groupKeyMappingB, groupKeyMappingC, tuple), collector,
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
