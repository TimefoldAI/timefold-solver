package ai.timefold.solver.constraint.streams.bavet.quad;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.config.solver.EnvironmentMode;

final class Group1Mapping0CollectorQuadNode<OldA, OldB, OldC, OldD, A>
        extends AbstractGroupQuadNode<OldA, OldB, OldC, OldD, UniTuple<A>, A, Void, Void> {

    private final int outputStoreSize;

    public Group1Mapping0CollectorQuadNode(QuadFunction<OldA, OldB, OldC, OldD, A> groupKeyMapping,
            int groupStoreIndex, int dirtyListPositionStoreIndex,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode) {
        super(groupStoreIndex, dirtyListPositionStoreIndex,
                tuple -> createGroupKey(groupKeyMapping, tuple), nextNodesTupleLifecycle, environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    static <A, OldA, OldB, OldC, OldD> A createGroupKey(QuadFunction<OldA, OldB, OldC, OldD, A> groupKeyMapping,
            QuadTuple<OldA, OldB, OldC, OldD> tuple) {
        return groupKeyMapping.apply(tuple.factA, tuple.factB, tuple.factC, tuple.factD);
    }

    @Override
    protected UniTuple<A> createOutTuple(A a) {
        return new UniTuple<>(a, outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(UniTuple<A> aUniTuple, Void unused) {
        throw new IllegalStateException("Impossible state: collector is null.");
    }

}
