package ai.timefold.solver.constraint.streams.bavet.bi;

import ai.timefold.solver.constraint.streams.bavet.common.TupleLifecycle;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.util.Pair;

final class Group0Mapping2CollectorBiNode<OldA, OldB, A, B, ResultContainerA_, ResultContainerB_>
        extends AbstractGroupBiNode<OldA, OldB, BiTuple<A, B>, BiTupleImpl<A, B>, Void, Object, Pair<A, B>> {

    private final int outputStoreSize;

    public Group0Mapping2CollectorBiNode(int groupStoreIndex, int undoStoreIndex,
            BiConstraintCollector<OldA, OldB, ResultContainerA_, A> collectorA,
            BiConstraintCollector<OldA, OldB, ResultContainerB_, B> collectorB,
            TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex, null, mergeCollectors(collectorA, collectorB), nextNodesTupleLifecycle,
                environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    static <OldA, OldB, A, B, ResultContainerA_, ResultContainerB_>
            BiConstraintCollector<OldA, OldB, Object, Pair<A, B>> mergeCollectors(
                    BiConstraintCollector<OldA, OldB, ResultContainerA_, A> collectorA,
                    BiConstraintCollector<OldA, OldB, ResultContainerB_, B> collectorB) {
        return (BiConstraintCollector<OldA, OldB, Object, Pair<A, B>>) ConstraintCollectors.compose(collectorA, collectorB,
                Pair::of);
    }

    @Override
    protected BiTupleImpl<A, B> createOutTuple(Void groupKey) {
        return new BiTupleImpl<>(null, null, outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(BiTupleImpl<A, B> outTuple, Pair<A, B> result) {
        outTuple.factA = result.getKey();
        outTuple.factB = result.getValue();
    }

}
