package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import static ai.timefold.solver.core.impl.score.stream.bavet.uni.Group2Mapping0CollectorUniNode.createGroupKey;

import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.util.Pair;

final class Group2Mapping1CollectorUniNode<OldA, A, B, C, ResultContainer_>
        extends AbstractGroupUniNode<OldA, TriTuple<A, B, C>, Pair<A, B>, ResultContainer_, C> {

    private final int outputStoreSize;

    public Group2Mapping1CollectorUniNode(Function<OldA, A> groupKeyMappingA, Function<OldA, B> groupKeyMappingB,
            int groupStoreIndex, int undoStoreIndex,
            UniConstraintCollector<OldA, ResultContainer_, C> collector,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex,
                tuple -> createGroupKey(groupKeyMappingA, groupKeyMappingB, tuple), collector,
                nextNodesTupleLifecycle, environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected TriTuple<A, B, C> createOutTuple(Pair<A, B> groupKey) {
        return new TriTuple<>(groupKey.key(), groupKey.value(), null, outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(TriTuple<A, B, C> outTuple, C c) {
        outTuple.factC = c;
    }

}
