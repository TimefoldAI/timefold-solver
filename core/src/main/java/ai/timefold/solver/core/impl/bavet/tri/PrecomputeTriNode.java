package ai.timefold.solver.core.impl.bavet.tri;

import java.util.function.Supplier;

import ai.timefold.solver.core.impl.bavet.common.AbstractPrecomputeNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetPrecomputeBuildHelper;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class PrecomputeTriNode<A, B, C> extends AbstractPrecomputeNode<TriTuple<A, B, C>> {
    private final int outputStoreSize;

    public PrecomputeTriNode(Supplier<BavetPrecomputeBuildHelper<TriTuple<A, B, C>>> precomputeBuildHelperSupplier,
            int outputStoreSize,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle,
            Class<?>[] sourceClasses) {
        super(precomputeBuildHelperSupplier, nextNodesTupleLifecycle, sourceClasses);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected TriTuple<A, B, C> remapTuple(TriTuple<A, B, C> tuple) {
        return new TriTuple<>(tuple.factA, tuple.factB, tuple.factC, outputStoreSize);
    }
}
