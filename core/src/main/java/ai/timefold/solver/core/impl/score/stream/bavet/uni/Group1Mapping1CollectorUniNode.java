package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import static ai.timefold.solver.core.impl.score.stream.bavet.uni.Group1Mapping0CollectorUniNode.createGroupKey;

import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;

final class Group1Mapping1CollectorUniNode<OldA, A, B, ResultContainer_>
        extends AbstractGroupUniNode<OldA, BiTuple<A, B>, A, ResultContainer_, B> {

    private final int outputStoreSize;

    public Group1Mapping1CollectorUniNode(Function<OldA, A> groupKeyMapping,
            int groupStoreIndex, int undoStoreIndex,
            UniConstraintCollector<OldA, ResultContainer_, B> collector,
            TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex,
                tuple -> createGroupKey(groupKeyMapping, tuple), collector,
                nextNodesTupleLifecycle, environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected BiTuple<A, B> createOutTuple(A a) {
        return new BiTuple<>(a, null, outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(BiTuple<A, B> outTuple, B b) {
        outTuple.factB = b;
    }

}
