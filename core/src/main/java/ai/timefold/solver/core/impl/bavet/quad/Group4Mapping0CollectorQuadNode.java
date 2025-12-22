package ai.timefold.solver.core.impl.bavet.quad;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.util.Quadruple;

public final class Group4Mapping0CollectorQuadNode<OldA, OldB, OldC, OldD, A, B, C, D>
        extends
        AbstractGroupQuadNode<OldA, OldB, OldC, OldD, QuadTuple<A, B, C, D>, Quadruple<A, B, C, D>, Void, Void> {

    private final int outputStoreSize;

    public Group4Mapping0CollectorQuadNode(QuadFunction<OldA, OldB, OldC, OldD, A> groupKeyMappingA,
            QuadFunction<OldA, OldB, OldC, OldD, B> groupKeyMappingB, QuadFunction<OldA, OldB, OldC, OldD, C> groupKeyMappingC,
            QuadFunction<OldA, OldB, OldC, OldD, D> groupKeyMappingD,
            int groupStoreIndex,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle, int outputStoreSize,
            EnvironmentMode environmentMode) {
        super(groupStoreIndex,
                tuple -> createGroupKey(groupKeyMappingA, groupKeyMappingB, groupKeyMappingC, groupKeyMappingD, tuple),
                nextNodesTupleLifecycle, environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    private static <A, B, C, D, OldA, OldB, OldC, OldD> Quadruple<A, B, C, D> createGroupKey(
            QuadFunction<OldA, OldB, OldC, OldD, A> groupKeyMappingA,
            QuadFunction<OldA, OldB, OldC, OldD, B> groupKeyMappingB,
            QuadFunction<OldA, OldB, OldC, OldD, C> groupKeyMappingC,
            QuadFunction<OldA, OldB, OldC, OldD, D> groupKeyMappingD,
            QuadTuple<OldA, OldB, OldC, OldD> tuple) {
        var oldA = tuple.getA();
        var oldB = tuple.getB();
        var oldC = tuple.getC();
        var oldD = tuple.getD();
        return new Quadruple<>(groupKeyMappingA.apply(oldA, oldB, oldC, oldD), groupKeyMappingB.apply(oldA, oldB, oldC, oldD),
                groupKeyMappingC.apply(oldA, oldB, oldC, oldD), groupKeyMappingD.apply(oldA, oldB, oldC, oldD));
    }

    @Override
    protected QuadTuple<A, B, C, D> createOutTuple(Quadruple<A, B, C, D> groupKey) {
        return QuadTuple.of(groupKey.a(), groupKey.b(), groupKey.c(), groupKey.d(), outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(QuadTuple<A, B, C, D> outTuple, Void unused) {
        throw new IllegalStateException("Impossible state: collector is null.");
    }

}
