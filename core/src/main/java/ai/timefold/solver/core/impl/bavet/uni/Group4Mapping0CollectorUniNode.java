package ai.timefold.solver.core.impl.bavet.uni;

import java.util.function.Function;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.util.Quadruple;

public final class Group4Mapping0CollectorUniNode<OldA, A, B, C, D>
        extends
        AbstractGroupUniNode<OldA, QuadTuple<A, B, C, D>, Quadruple<A, B, C, D>, Void, Void> {

    private final int outputStoreSize;

    public Group4Mapping0CollectorUniNode(Function<OldA, A> groupKeyMappingA, Function<OldA, B> groupKeyMappingB,
            Function<OldA, C> groupKeyMappingC, Function<OldA, D> groupKeyMappingD,
            int groupStoreIndex,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle, int outputStoreSize,
            EnvironmentMode environmentMode) {
        super(groupStoreIndex,
                tuple -> createGroupKey(groupKeyMappingA, groupKeyMappingB, groupKeyMappingC, groupKeyMappingD, tuple),
                nextNodesTupleLifecycle, environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    private static <A, B, C, D, OldA> Quadruple<A, B, C, D> createGroupKey(Function<OldA, A> groupKeyMappingA,
            Function<OldA, B> groupKeyMappingB, Function<OldA, C> groupKeyMappingC, Function<OldA, D> groupKeyMappingD,
            UniTuple<OldA> tuple) {
        var oldA = tuple.getA();
        return new Quadruple<>(groupKeyMappingA.apply(oldA), groupKeyMappingB.apply(oldA), groupKeyMappingC.apply(oldA),
                groupKeyMappingD.apply(oldA));
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
