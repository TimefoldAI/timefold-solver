package ai.timefold.solver.constraint.streams.bavet.quad;

import static ai.timefold.solver.constraint.streams.bavet.quad.Group1Mapping0CollectorQuadNode.createGroupKey;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.BiTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;

final class Group1Mapping1CollectorQuadNode<OldA, OldB, OldC, OldD, A, B, ResultContainer_>
        extends AbstractGroupQuadNode<OldA, OldB, OldC, OldD, BiTuple<A, B>, A, ResultContainer_, B> {

    private final int outputStoreSize;

    public Group1Mapping1CollectorQuadNode(QuadFunction<OldA, OldB, OldC, OldD, A> groupKeyMapping, int groupStoreIndex,
            int undoStoreIndex, QuadConstraintCollector<OldA, OldB, OldC, OldD, ResultContainer_, B> collector,
            TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex, tuple -> createGroupKey(groupKeyMapping, tuple), collector,
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
