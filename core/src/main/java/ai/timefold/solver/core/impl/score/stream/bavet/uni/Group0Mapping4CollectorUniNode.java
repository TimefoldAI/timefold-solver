package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.util.Quadruple;

final class Group0Mapping4CollectorUniNode<OldA, A, B, C, D, ResultContainerA_, ResultContainerB_, ResultContainerC_, ResultContainerD_>
        extends
        AbstractGroupUniNode<OldA, QuadTuple<A, B, C, D>, Void, Object, Quadruple<A, B, C, D>> {

    private final int outputStoreSize;

    public Group0Mapping4CollectorUniNode(int groupStoreIndex, int undoStoreIndex,
            UniConstraintCollector<OldA, ResultContainerA_, A> collectorA,
            UniConstraintCollector<OldA, ResultContainerB_, B> collectorB,
            UniConstraintCollector<OldA, ResultContainerC_, C> collectorC,
            UniConstraintCollector<OldA, ResultContainerD_, D> collectorD,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle, int outputStoreSize,
            EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex,
                null, mergeCollectors(collectorA, collectorB, collectorC, collectorD),
                nextNodesTupleLifecycle, environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    private static <OldA, A, B, C, D, ResultContainerA_, ResultContainerB_, ResultContainerC_, ResultContainerD_>
            UniConstraintCollector<OldA, Object, Quadruple<A, B, C, D>> mergeCollectors(
                    UniConstraintCollector<OldA, ResultContainerA_, A> collectorA,
                    UniConstraintCollector<OldA, ResultContainerB_, B> collectorB,
                    UniConstraintCollector<OldA, ResultContainerC_, C> collectorC,
                    UniConstraintCollector<OldA, ResultContainerD_, D> collectorD) {
        return (UniConstraintCollector<OldA, Object, Quadruple<A, B, C, D>>) ConstraintCollectors.compose(collectorA,
                collectorB, collectorC, collectorD, Quadruple::new);
    }

    @Override
    protected QuadTuple<A, B, C, D> createOutTuple(Void groupKey) {
        return new QuadTuple<>(null, null, null, null, outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(QuadTuple<A, B, C, D> outTuple, Quadruple<A, B, C, D> result) {
        outTuple.factA = result.a();
        outTuple.factB = result.b();
        outTuple.factC = result.c();
        outTuple.factD = result.d();
    }

}
