package ai.timefold.solver.core.impl.bavet.bi;

import java.util.function.Supplier;

import ai.timefold.solver.core.impl.bavet.common.AbstractPrecomputeNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetPrecomputeBuildHelper;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class PrecomputeBiNode<A, B> extends AbstractPrecomputeNode<BiTuple<A, B>> {
    private final int outputStoreSize;

    public PrecomputeBiNode(Supplier<BavetPrecomputeBuildHelper<BiTuple<A, B>>> precomputeBuildHelperSupplier,
            int outputStoreSize,
            TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle,
            Class<?>[] sourceClasses) {
        super(precomputeBuildHelperSupplier, nextNodesTupleLifecycle, sourceClasses);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected BiTuple<A, B> remapTuple(BiTuple<A, B> tuple) {
        return new BiTuple<>(tuple.factA, tuple.factB, outputStoreSize);
    }
}
