package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractLoadBalanceSlot;
import ai.timefold.solver.core.impl.score.stream.collector.DefaultLoadBalance;

import org.jspecify.annotations.NonNull;

final class LoadBalanceTriCollector<A, B, C, Balanced_>
        implements TriConstraintCollector<A, B, C, DefaultLoadBalance<Balanced_>, LoadBalance<Balanced_>> {

    private final TriFunction<A, B, C, Balanced_> balancedItemFunction;
    private final ToLongTriFunction<A, B, C> loadFunction;
    private final ToLongTriFunction<A, B, C> initialLoadFunction;

    public LoadBalanceTriCollector(TriFunction<A, B, C, Balanced_> balancedItemFunction,
            ToLongTriFunction<A, B, C> loadFunction,
            ToLongTriFunction<A, B, C> initialLoadFunction) {
        this.balancedItemFunction = balancedItemFunction;
        this.loadFunction = loadFunction;
        this.initialLoadFunction = initialLoadFunction;
    }

    @Override
    public @NonNull Supplier<DefaultLoadBalance<Balanced_>> supplier() {
        return DefaultLoadBalance::new;
    }

    @Override
    public @NonNull QuadFunction<DefaultLoadBalance<Balanced_>, A, B, C, Runnable> accumulator() {
        return TriCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull TriConstraintCollectorAccumulator<DefaultLoadBalance<Balanced_>, A, B, C> incrementalAccumulator() {
        return Slot::new;
    }

    @Override
    public @NonNull Function<DefaultLoadBalance<Balanced_>, LoadBalance<Balanced_>> finisher() {
        return balanceStatistics -> balanceStatistics;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LoadBalanceTriCollector<?, ?, ?, ?> that
                && Objects.equals(balancedItemFunction, that.balancedItemFunction)
                && Objects.equals(loadFunction, that.loadFunction)
                && Objects.equals(initialLoadFunction, that.initialLoadFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(balancedItemFunction, loadFunction, initialLoadFunction);
    }

    private final class Slot extends AbstractLoadBalanceSlot<Balanced_>
            implements TriConstraintCollectorValueHandle<A, B, C> {

        Slot(DefaultLoadBalance<Balanced_> container) {
            super(container);
        }

        @Override
        public void add(A a, B b, C c) {
            addMapped(balancedItemFunction.apply(a, b, c), loadFunction.applyAsLong(a, b, c),
                    initialLoadFunction.applyAsLong(a, b, c));
        }

        @Override
        public void replaceWith(A a, B b, C c) {
            replaceWithMapped(balancedItemFunction.apply(a, b, c), loadFunction.applyAsLong(a, b, c),
                    initialLoadFunction.applyAsLong(a, b, c));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
