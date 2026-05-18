package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorValueHandle;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractLoadBalanceSlot;
import ai.timefold.solver.core.impl.score.stream.collector.DefaultLoadBalance;

import org.jspecify.annotations.NonNull;

final class LoadBalanceBiCollector<A, B, Balanced_>
        implements BiConstraintCollector<A, B, DefaultLoadBalance<Balanced_>, LoadBalance<Balanced_>> {

    private final BiFunction<A, B, Balanced_> balancedItemFunction;
    private final ToLongBiFunction<A, B> loadFunction;
    private final ToLongBiFunction<A, B> initialLoadFunction;

    public LoadBalanceBiCollector(BiFunction<A, B, Balanced_> balancedItemFunction, ToLongBiFunction<A, B> loadFunction,
            ToLongBiFunction<A, B> initialLoadFunction) {
        this.balancedItemFunction = balancedItemFunction;
        this.loadFunction = loadFunction;
        this.initialLoadFunction = initialLoadFunction;
    }

    @Override
    public @NonNull Supplier<DefaultLoadBalance<Balanced_>> supplier() {
        return DefaultLoadBalance::new;
    }

    @Override
    public @NonNull BiConstraintCollectorAccumulator<DefaultLoadBalance<Balanced_>, A, B> accumulator() {
        return Slot::new;
    }

    @Override
    public @NonNull Function<DefaultLoadBalance<Balanced_>, LoadBalance<Balanced_>> finisher() {
        return balanceStatistics -> balanceStatistics;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LoadBalanceBiCollector<?, ?, ?> that
                && Objects.equals(balancedItemFunction, that.balancedItemFunction)
                && Objects.equals(loadFunction, that.loadFunction)
                && Objects.equals(initialLoadFunction, that.initialLoadFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(balancedItemFunction, loadFunction, initialLoadFunction);
    }

    private final class Slot extends AbstractLoadBalanceSlot<Balanced_>
            implements BiConstraintCollectorValueHandle<A, B> {

        Slot(DefaultLoadBalance<Balanced_> container) {
            super(container);
        }

        @Override
        public void add(A a, B b) {
            addMapped(balancedItemFunction.apply(a, b), loadFunction.applyAsLong(a, b),
                    initialLoadFunction.applyAsLong(a, b));
        }

        @Override
        public void replaceWith(A a, B b) {
            replaceWithMapped(balancedItemFunction.apply(a, b), loadFunction.applyAsLong(a, b),
                    initialLoadFunction.applyAsLong(a, b));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
