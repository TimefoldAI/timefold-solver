package ai.timefold.solver.core.impl.bavet.tri;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.util.Pair;

public final class Group2Mapping0CollectorTriNode<OldA, OldB, OldC, A, B>
        extends AbstractGroupTriNode<OldA, OldB, OldC, BiTuple<A, B>, Pair<A, B>, Void, Void> {

    private final int outputStoreSize;

    public Group2Mapping0CollectorTriNode(TriFunction<OldA, OldB, OldC, A> groupKeyMappingA,
            TriFunction<OldA, OldB, OldC, B> groupKeyMappingB, int groupStoreIndex,
            TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode) {
        super(groupStoreIndex,
                tuple -> createGroupKey(groupKeyMappingA, groupKeyMappingB, tuple), nextNodesTupleLifecycle, environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    static <A, B, OldA, OldB, OldC> Pair<A, B> createGroupKey(TriFunction<OldA, OldB, OldC, A> groupKeyMappingA,
            TriFunction<OldA, OldB, OldC, B> groupKeyMappingB, TriTuple<OldA, OldB, OldC> tuple) {
        var oldA = tuple.getA();
        var oldB = tuple.getB();
        var oldC = tuple.getC();
        return new Pair<>(groupKeyMappingA.apply(oldA, oldB, oldC), groupKeyMappingB.apply(oldA, oldB, oldC));
    }

    @Override
    protected BiTuple<A, B> createOutTuple(Pair<A, B> groupKey) {
        return BiTuple.of(groupKey.key(), groupKey.value(), outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(BiTuple<A, B> outTuple, Void unused) {
        throw new IllegalStateException("Impossible state: collector is null.");
    }

}
