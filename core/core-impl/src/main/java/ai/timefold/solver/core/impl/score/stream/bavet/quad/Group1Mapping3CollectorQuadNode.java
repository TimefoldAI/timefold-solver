package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.util.Triple;

final class Group1Mapping3CollectorQuadNode<OldA, OldB, OldC, OldD, A, B, C, D, ResultContainerB_, ResultContainerC_, ResultContainerD_>
        extends
        AbstractGroupQuadNode<OldA, OldB, OldC, OldD, QuadTuple<A, B, C, D>, A, Object, Triple<B, C, D>> {

    private final int outputStoreSize;

    public Group1Mapping3CollectorQuadNode(QuadFunction<OldA, OldB, OldC, OldD, A> groupKeyMapping,
            int groupStoreIndex, int undoStoreIndex,
            QuadConstraintCollector<OldA, OldB, OldC, OldD, ResultContainerB_, B> collectorB,
            QuadConstraintCollector<OldA, OldB, OldC, OldD, ResultContainerC_, C> collectorC,
            QuadConstraintCollector<OldA, OldB, OldC, OldD, ResultContainerD_, D> collectorD,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle, int outputStoreSize,
            EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex, tuple -> Group1Mapping0CollectorQuadNode.createGroupKey(groupKeyMapping, tuple),
                Group0Mapping3CollectorQuadNode.mergeCollectors(collectorB, collectorC, collectorD), nextNodesTupleLifecycle,
                environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected QuadTuple<A, B, C, D> createOutTuple(A a) {
        return new QuadTuple<>(a, null, null, null, outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(QuadTuple<A, B, C, D> outTuple, Triple<B, C, D> result) {
        outTuple.factB = result.a();
        outTuple.factC = result.b();
        outTuple.factD = result.c();
    }

}
