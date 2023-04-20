package ai.timefold.solver.constraint.streams.bavet.quad;

import static ai.timefold.solver.constraint.streams.bavet.quad.Group1Mapping0CollectorQuadNode.createGroupKey;

import ai.timefold.solver.constraint.streams.bavet.common.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.tri.TriTuple;
import ai.timefold.solver.constraint.streams.bavet.tri.TriTupleImpl;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.util.Pair;

final class Group1Mapping2CollectorQuadNode<OldA, OldB, OldC, OldD, A, B, C, ResultContainerB_, ResultContainerC_>
        extends AbstractGroupQuadNode<OldA, OldB, OldC, OldD, TriTuple<A, B, C>, TriTupleImpl<A, B, C>, A, Object, Pair<B, C>> {

    private final int outputStoreSize;

    public Group1Mapping2CollectorQuadNode(QuadFunction<OldA, OldB, OldC, OldD, A> groupKeyMapping, int groupStoreIndex,
            int undoStoreIndex, QuadConstraintCollector<OldA, OldB, OldC, OldD, ResultContainerB_, B> collectorB,
            QuadConstraintCollector<OldA, OldB, OldC, OldD, ResultContainerC_, C> collectorC,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex, tuple -> createGroupKey(groupKeyMapping, tuple),
                Group0Mapping2CollectorQuadNode.mergeCollectors(collectorB, collectorC), nextNodesTupleLifecycle,
                environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected TriTupleImpl<A, B, C> createOutTuple(A a) {
        return new TriTupleImpl<>(a, null, null, outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(TriTupleImpl<A, B, C> outTuple, Pair<B, C> result) {
        outTuple.factB = result.getKey();
        outTuple.factC = result.getValue();
    }

}
