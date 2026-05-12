package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractLoadBalanceSlot;
import ai.timefold.solver.core.impl.score.stream.collector.DefaultLoadBalance;

import org.jspecify.annotations.NonNull;

final class LoadBalanceUniCollector<A, Balanced_>
        implements UniConstraintCollector<A, DefaultLoadBalance<Balanced_>, LoadBalance<Balanced_>> {

    private final Function<A, Balanced_> balancedItemFunction;
    private final ToLongFunction<A> loadFunction;
    private final ToLongFunction<A> initialLoadFunction;

    public LoadBalanceUniCollector(Function<A, Balanced_> balancedItemFunction, ToLongFunction<A> loadFunction,
            ToLongFunction<A> initialLoadFunction) {
        this.balancedItemFunction = balancedItemFunction;
        this.loadFunction = loadFunction;
        this.initialLoadFunction = initialLoadFunction;
    }

    @Override
    public @NonNull Supplier<DefaultLoadBalance<Balanced_>> supplier() {
        return DefaultLoadBalance::new;
    }

    @Override
    public @NonNull BiFunction<DefaultLoadBalance<Balanced_>, A, Runnable> accumulator() {
        return UniCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull UniConstraintCollectorAccumulator<DefaultLoadBalance<Balanced_>, A> incrementalAccumulator() {
        return Slot::new;
    }

    @Override
    public @NonNull Function<DefaultLoadBalance<Balanced_>, LoadBalance<Balanced_>> finisher() {
        return balanceStatistics -> balanceStatistics;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LoadBalanceUniCollector<?, ?> that
                && Objects.equals(balancedItemFunction, that.balancedItemFunction)
                && Objects.equals(loadFunction, that.loadFunction)
                && Objects.equals(initialLoadFunction, that.initialLoadFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(balancedItemFunction, loadFunction, initialLoadFunction);
    }

    private final class Slot extends AbstractLoadBalanceSlot<Balanced_>
            implements UniConstraintCollectorValueHandle<A> {

        Slot(DefaultLoadBalance<Balanced_> container) {
            super(container);
        }

        @Override
        public void add(A a) {
            addMapped(balancedItemFunction.apply(a), loadFunction.applyAsLong(a), initialLoadFunction.applyAsLong(a));
        }

        @Override
        public void update(A a) {
            updateMapped(balancedItemFunction.apply(a), loadFunction.applyAsLong(a), initialLoadFunction.applyAsLong(a));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
