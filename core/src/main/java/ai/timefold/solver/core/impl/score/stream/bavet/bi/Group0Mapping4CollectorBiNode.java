package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.util.Quadruple;

final class Group0Mapping4CollectorBiNode<OldA, OldB, A, B, C, D, ResultContainerA_, ResultContainerB_, ResultContainerC_, ResultContainerD_>
        extends
        AbstractGroupBiNode<OldA, OldB, QuadTuple<A, B, C, D>, Void, Object, Quadruple<A, B, C, D>> {

    private final int outputStoreSize;

    public Group0Mapping4CollectorBiNode(int groupStoreIndex, int undoStoreIndex,
            BiConstraintCollector<OldA, OldB, ResultContainerA_, A> collectorA,
            BiConstraintCollector<OldA, OldB, ResultContainerB_, B> collectorB,
            BiConstraintCollector<OldA, OldB, ResultContainerC_, C> collectorC,
            BiConstraintCollector<OldA, OldB, ResultContainerD_, D> collectorD,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle, int outputStoreSize,
            EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex,
                null, mergeCollectors(collectorA, collectorB, collectorC, collectorD),
                nextNodesTupleLifecycle, environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    private static <OldA, OldB, A, B, C, D, ResultContainerA_, ResultContainerB_, ResultContainerC_, ResultContainerD_>
            BiConstraintCollector<OldA, OldB, Object, Quadruple<A, B, C, D>> mergeCollectors(
                    BiConstraintCollector<OldA, OldB, ResultContainerA_, A> collectorA,
                    BiConstraintCollector<OldA, OldB, ResultContainerB_, B> collectorB,
                    BiConstraintCollector<OldA, OldB, ResultContainerC_, C> collectorC,
                    BiConstraintCollector<OldA, OldB, ResultContainerD_, D> collectorD) {
        return (BiConstraintCollector<OldA, OldB, Object, Quadruple<A, B, C, D>>) ConstraintCollectors.compose(collectorA,
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
