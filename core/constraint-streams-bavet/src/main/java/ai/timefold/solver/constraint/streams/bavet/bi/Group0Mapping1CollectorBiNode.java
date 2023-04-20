package ai.timefold.solver.constraint.streams.bavet.bi;

import ai.timefold.solver.constraint.streams.bavet.common.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.uni.UniTuple;
import ai.timefold.solver.constraint.streams.bavet.uni.UniTupleImpl;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;

final class Group0Mapping1CollectorBiNode<OldA, OldB, A, ResultContainer_>
        extends AbstractGroupBiNode<OldA, OldB, UniTuple<A>, UniTupleImpl<A>, Void, ResultContainer_, A> {

    private final int outputStoreSize;

    public Group0Mapping1CollectorBiNode(int groupStoreIndex, int undoStoreIndex,
            BiConstraintCollector<OldA, OldB, ResultContainer_, A> collector,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode) {
        super(groupStoreIndex, undoStoreIndex, null, collector, nextNodesTupleLifecycle, environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected UniTupleImpl<A> createOutTuple(Void groupKey) {
        return new UniTupleImpl<>(null, outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(UniTupleImpl<A> outTuple, A a) {
        outTuple.factA = a;
    }

}
