package ai.timefold.solver.core.impl.bavet.common;

import java.util.function.Supplier;

import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetPrecomputeBuildHelper;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class AbstractPrecomputeNode<Tuple_ extends Tuple> extends AbstractNode
        implements BavetRootNode<Object> {
    private final RecordAndReplayPropagator<Tuple_> recordAndReplayPropagator;
    private final Class<?>[] sourceClasses;

    protected AbstractPrecomputeNode(Supplier<BavetPrecomputeBuildHelper<Tuple_>> precomputeBuildHelperSupplier,
            TupleLifecycle<Tuple_> nextNodesTupleLifecycle,
            Class<?>[] sourceClasses) {
        this.recordAndReplayPropagator = new RecordAndReplayPropagator<>(precomputeBuildHelperSupplier,
                this::remapTuple,
                nextNodesTupleLifecycle);
        this.sourceClasses = sourceClasses;
    }

    @Override
    public StreamKind getStreamKind() {
        return StreamKind.PRECOMPUTE;
    }

    @Override
    public final Propagator getPropagator() {
        return recordAndReplayPropagator;
    }

    @Override
    public final boolean allowsInstancesOf(Class<?> clazz) {
        for (var sourceClass : sourceClasses) {
            if (sourceClass.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final Class<?>[] getSourceClasses() {
        return sourceClasses;
    }

    @Override
    public final boolean supports(BavetRootNode.LifecycleOperation lifecycleOperation) {
        return true;
    }

    @Override
    public final void insert(@Nullable Object a) {
        if (a == null) {
            return;
        }
        recordAndReplayPropagator.insert(a);
    }

    @Override
    public final void update(@Nullable Object a) {
        if (a == null) {
            return;
        }
        recordAndReplayPropagator.update(a);
    }

    @Override
    public final void retract(@Nullable Object a) {
        if (a == null) {
            return;
        }
        recordAndReplayPropagator.retract(a);
    }

    protected abstract Tuple_ remapTuple(Tuple_ tuple);
}
