package ai.timefold.solver.core.impl.bavet.uni;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class Group0Mapping1CollectorUniNode<OldA, A, ResultContainer_>
        extends AbstractGroupUniNode<OldA, UniTuple<A>, Void, ResultContainer_, A> {

    private final int outputStoreSize;

    public Group0Mapping1CollectorUniNode(int groupStoreIndex, int undoStoreIndex,
            UniConstraintCollector<OldA, ResultContainer_, A> collector,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex,
                null, collector, nextNodesTupleLifecycle, environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected UniTuple<A> createOutTuple(Void groupKey) {
        return new UniTuple<>(null, outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(UniTuple<A> outTuple, A a) {
        outTuple.factA = a;
    }

}
