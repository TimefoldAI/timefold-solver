package ai.timefold.solver.core.impl.bavet.bi;

import java.util.function.BiFunction;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.util.Quadruple;

public final class Group4Mapping0CollectorBiNode<OldA, OldB, A, B, C, D>
        extends
        AbstractGroupBiNode<OldA, OldB, QuadTuple<A, B, C, D>, Quadruple<A, B, C, D>, Void, Void> {

    private final int outputStoreSize;

    public Group4Mapping0CollectorBiNode(BiFunction<OldA, OldB, A> groupKeyMappingA, BiFunction<OldA, OldB, B> groupKeyMappingB,
            BiFunction<OldA, OldB, C> groupKeyMappingC, BiFunction<OldA, OldB, D> groupKeyMappingD,
            int groupStoreIndex,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle, int outputStoreSize,
            EnvironmentMode environmentMode) {
        super(groupStoreIndex,
                tuple -> createGroupKey(groupKeyMappingA, groupKeyMappingB, groupKeyMappingC, groupKeyMappingD, tuple),
                nextNodesTupleLifecycle, environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    private static <A, B, C, D, OldA, OldB> Quadruple<A, B, C, D> createGroupKey(
            BiFunction<OldA, OldB, A> groupKeyMappingA, BiFunction<OldA, OldB, B> groupKeyMappingB,
            BiFunction<OldA, OldB, C> groupKeyMappingC, BiFunction<OldA, OldB, D> groupKeyMappingD,
            BiTuple<OldA, OldB> tuple) {
        var oldA = tuple.getA();
        var oldB = tuple.getB();
        return new Quadruple<>(groupKeyMappingA.apply(oldA, oldB), groupKeyMappingB.apply(oldA, oldB),
                groupKeyMappingC.apply(oldA, oldB), groupKeyMappingD.apply(oldA, oldB));
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
