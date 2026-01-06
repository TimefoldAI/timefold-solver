package ai.timefold.solver.core.impl.bavet.quad;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.util.Triple;

public final class Group3Mapping0CollectorQuadNode<OldA, OldB, OldC, OldD, A, B, C>
        extends
        AbstractGroupQuadNode<OldA, OldB, OldC, OldD, TriTuple<A, B, C>, Triple<A, B, C>, Void, Void> {

    private final int outputStoreSize;

    public Group3Mapping0CollectorQuadNode(QuadFunction<OldA, OldB, OldC, OldD, A> groupKeyMappingA,
            QuadFunction<OldA, OldB, OldC, OldD, B> groupKeyMappingB, QuadFunction<OldA, OldB, OldC, OldD, C> groupKeyMappingC,
            int groupStoreIndex,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode) {
        super(groupStoreIndex,
                tuple -> createGroupKey(groupKeyMappingA, groupKeyMappingB, groupKeyMappingC, tuple),
                nextNodesTupleLifecycle, environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    static <A, B, C, OldA, OldB, OldC, OldD> Triple<A, B, C> createGroupKey(
            QuadFunction<OldA, OldB, OldC, OldD, A> groupKeyMappingA,
            QuadFunction<OldA, OldB, OldC, OldD, B> groupKeyMappingB,
            QuadFunction<OldA, OldB, OldC, OldD, C> groupKeyMappingC,
            QuadTuple<OldA, OldB, OldC, OldD> tuple) {
        var oldA = tuple.getA();
        var oldB = tuple.getB();
        var oldC = tuple.getC();
        var oldD = tuple.getD();
        return new Triple<>(groupKeyMappingA.apply(oldA, oldB, oldC, oldD), groupKeyMappingB.apply(oldA, oldB, oldC, oldD),
                groupKeyMappingC.apply(oldA, oldB, oldC, oldD));
    }

    @Override
    protected TriTuple<A, B, C> createOutTuple(Triple<A, B, C> groupKey) {
        return TriTuple.of(groupKey.a(), groupKey.b(), groupKey.c(), outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(TriTuple<A, B, C> outTuple, Void unused) {
        throw new IllegalStateException("Impossible state: collector is null.");
    }

}
