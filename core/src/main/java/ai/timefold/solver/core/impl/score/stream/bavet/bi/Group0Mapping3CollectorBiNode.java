package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.util.Triple;

final class Group0Mapping3CollectorBiNode<OldA, OldB, A, B, C, ResultContainerA_, ResultContainerB_, ResultContainerC_>
        extends AbstractGroupBiNode<OldA, OldB, TriTuple<A, B, C>, Void, Object, Triple<A, B, C>> {

    private final int outputStoreSize;

    public Group0Mapping3CollectorBiNode(int groupStoreIndex, int undoStoreIndex,
            BiConstraintCollector<OldA, OldB, ResultContainerA_, A> collectorA,
            BiConstraintCollector<OldA, OldB, ResultContainerB_, B> collectorB,
            BiConstraintCollector<OldA, OldB, ResultContainerC_, C> collectorC,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex,
                null, mergeCollectors(collectorA, collectorB, collectorC),
                nextNodesTupleLifecycle, environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    static <OldA, OldB, A, B, C, ResultContainerA_, ResultContainerB_, ResultContainerC_>
            BiConstraintCollector<OldA, OldB, Object, Triple<A, B, C>> mergeCollectors(
                    BiConstraintCollector<OldA, OldB, ResultContainerA_, A> collectorA,
                    BiConstraintCollector<OldA, OldB, ResultContainerB_, B> collectorB,
                    BiConstraintCollector<OldA, OldB, ResultContainerC_, C> collectorC) {
        return (BiConstraintCollector<OldA, OldB, Object, Triple<A, B, C>>) ConstraintCollectors.compose(collectorA, collectorB,
                collectorC, Triple::new);
    }

    @Override
    protected TriTuple<A, B, C> createOutTuple(Void groupKey) {
        return new TriTuple<>(null, null, null, outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(TriTuple<A, B, C> outTuple, Triple<A, B, C> result) {
        outTuple.factA = result.a();
        outTuple.factB = result.b();
        outTuple.factC = result.c();
    }

}
