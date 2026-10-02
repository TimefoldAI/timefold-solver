package ai.timefold.solver.core.impl.bavet.tri;

import java.util.function.IntSupplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

public final class Group1Mapping1CollectorTriNode<OldA, OldB, OldC, A, B, ResultContainer_>
        extends AbstractGroupTriNode<OldA, OldB, OldC, BiTuple<A, B>, A, ResultContainer_, B> {

    private final int outputStoreSize;

    public Group1Mapping1CollectorTriNode(TriFunction<OldA, OldB, OldC, A> groupKeyMapping,
            IntSupplier storeIndexReserver,
            TriConstraintCollector<OldA, OldB, OldC, ResultContainer_, B> collector,
            TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode) {
        super(storeIndexReserver,
                tuple -> Group1Mapping0CollectorTriNode.createGroupKey(groupKeyMapping, tuple), collector,
                nextNodesTupleLifecycle, environmentMode);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected BiTuple<A, B> createOutTuple(A a) {
        return BiTuple.of(a, outputStoreSize);
    }

    @Override
    protected void updateOutTupleToResult(BiTuple<A, B> outTuple, B b) {
        outTuple.setB(b);
    }

}
