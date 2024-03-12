package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class Group0Mapping1CollectorQuadNode<OldA, OldB, OldC, OldD, A, ResultContainer_>
        extends AbstractGroupQuadNode<OldA, OldB, OldC, OldD, UniTuple<A>, Void, ResultContainer_, A> {

    private final int outputStoreSize;

    public Group0Mapping1CollectorQuadNode(int groupStoreIndex, int undoStoreIndex,
            QuadConstraintCollector<OldA, OldB, OldC, OldD, ResultContainer_, A> collector,
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
