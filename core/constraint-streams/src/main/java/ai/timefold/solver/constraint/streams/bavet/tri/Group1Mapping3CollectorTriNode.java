package ai.timefold.solver.constraint.streams.bavet.tri;

import static ai.timefold.solver.constraint.streams.bavet.tri.Group0Mapping3CollectorTriNode.mergeCollectors;
import static ai.timefold.solver.constraint.streams.bavet.tri.Group1Mapping0CollectorTriNode.createGroupKey;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.util.Triple;

final class Group1Mapping3CollectorTriNode<OldA, OldB, OldC, A, B, C, D, ResultContainerB_, ResultContainerC_, ResultContainerD_>
        extends
        AbstractGroupTriNode<OldA, OldB, OldC, QuadTuple<A, B, C, D>, A, Object, Triple<B, C, D>> {

    private final int outputStoreSize;

    public Group1Mapping3CollectorTriNode(TriFunction<OldA, OldB, OldC, A> groupKeyMapping,
            int groupStoreIndex, int undoStoreIndex, int dirtyListPositionStoreIndex,
            TriConstraintCollector<OldA, OldB, OldC, ResultContainerB_, B> collectorB,
            TriConstraintCollector<OldA, OldB, OldC, ResultContainerC_, C> collectorC,
            TriConstraintCollector<OldA, OldB, OldC, ResultContainerD_, D> collectorD,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle, int outputStoreSize,
            EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex, dirtyListPositionStoreIndex,
                tuple -> createGroupKey(groupKeyMapping, tuple),
                mergeCollectors(collectorB, collectorC, collectorD), nextNodesTupleLifecycle, environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected QuadTuple<A, B, C, D> createOutTuple(A a) {
        return new QuadTuple<>(a, null, null, null, outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(QuadTuple<A, B, C, D> outTuple, Triple<B, C, D> result) {
        outTuple.factB = result.getA();
        outTuple.factC = result.getB();
        outTuple.factD = result.getC();
    }

}
