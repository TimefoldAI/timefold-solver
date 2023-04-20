package ai.timefold.solver.constraint.streams.bavet.bi;

import java.util.function.BiFunction;

import ai.timefold.solver.constraint.streams.bavet.common.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.uni.UniTuple;
import ai.timefold.solver.constraint.streams.bavet.uni.UniTupleImpl;
import ai.timefold.solver.core.config.solver.EnvironmentMode;

final class Group1Mapping0CollectorBiNode<OldA, OldB, A>
        extends AbstractGroupBiNode<OldA, OldB, UniTuple<A>, UniTupleImpl<A>, A, Void, Void> {

    private final int outputStoreSize;

    public Group1Mapping0CollectorBiNode(BiFunction<OldA, OldB, A> groupKeyMapping, int groupStoreIndex,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode) {
        super(groupStoreIndex, tuple -> createGroupKey(groupKeyMapping, tuple), nextNodesTupleLifecycle, environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    static <A, OldA, OldB> A createGroupKey(BiFunction<OldA, OldB, A> groupKeyMapping, BiTuple<OldA, OldB> tuple) {
        return groupKeyMapping.apply(tuple.getFactA(), tuple.getFactB());
    }

    @Override
    protected UniTupleImpl<A> createOutTuple(A a) {
        return new UniTupleImpl<>(a, outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(UniTupleImpl<A> aUniTuple, Void unused) {
        throw new IllegalStateException("Impossible state: collector is null.");
    }

}
