package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.util.Pair;

final class Group1Mapping2CollectorUniNode<OldA, A, B, C, ResultContainerB_, ResultContainerC_>
        extends AbstractGroupUniNode<OldA, TriTuple<A, B, C>, A, Object, Pair<B, C>> {

    private final int outputStoreSize;

    public Group1Mapping2CollectorUniNode(Function<OldA, A> groupKeyMapping,
            int groupStoreIndex, int undoStoreIndex,
            UniConstraintCollector<OldA, ResultContainerB_, B> collectorB,
            UniConstraintCollector<OldA, ResultContainerC_, C> collectorC,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex,
                tuple -> Group1Mapping0CollectorUniNode.createGroupKey(groupKeyMapping, tuple),
                Group0Mapping2CollectorUniNode.mergeCollectors(collectorB, collectorC), nextNodesTupleLifecycle,
                environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected TriTuple<A, B, C> createOutTuple(A a) {
        return new TriTuple<>(a, null, null, outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(TriTuple<A, B, C> outTuple, Pair<B, C> result) {
        outTuple.factB = result.key();
        outTuple.factC = result.value();
    }

}
