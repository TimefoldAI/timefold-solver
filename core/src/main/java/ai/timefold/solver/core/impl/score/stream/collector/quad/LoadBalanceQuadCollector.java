package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractLoadBalanceSlot;
import ai.timefold.solver.core.impl.score.stream.collector.DefaultLoadBalance;

import org.jspecify.annotations.NonNull;

final class LoadBalanceQuadCollector<A, B, C, D, Balanced_>
        implements QuadConstraintCollector<A, B, C, D, DefaultLoadBalance<Balanced_>, LoadBalance<Balanced_>> {

    private final QuadFunction<A, B, C, D, Balanced_> balancedItemFunction;
    private final ToLongQuadFunction<A, B, C, D> loadFunction;
    private final ToLongQuadFunction<A, B, C, D> initialLoadFunction;

    public LoadBalanceQuadCollector(QuadFunction<A, B, C, D, Balanced_> balancedItemFunction,
            ToLongQuadFunction<A, B, C, D> loadFunction, ToLongQuadFunction<A, B, C, D> initialLoadFunction) {
        this.balancedItemFunction = balancedItemFunction;
        this.loadFunction = loadFunction;
        this.initialLoadFunction = initialLoadFunction;
    }

    @Override
    public @NonNull Supplier<DefaultLoadBalance<Balanced_>> supplier() {
        return DefaultLoadBalance::new;
    }

    @Override
    public @NonNull PentaFunction<DefaultLoadBalance<Balanced_>, A, B, C, D, Runnable> accumulator() {
        return QuadCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull QuadConstraintCollectorAccumulator<DefaultLoadBalance<Balanced_>, A, B, C, D> incrementalAccumulator() {
        return Slot::new;
    }

    @Override
    public @NonNull Function<DefaultLoadBalance<Balanced_>, LoadBalance<Balanced_>> finisher() {
        return balanceStatistics -> balanceStatistics;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LoadBalanceQuadCollector<?, ?, ?, ?, ?> that
                && Objects.equals(balancedItemFunction, that.balancedItemFunction)
                && Objects.equals(loadFunction, that.loadFunction)
                && Objects.equals(initialLoadFunction, that.initialLoadFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(balancedItemFunction, loadFunction, initialLoadFunction);
    }

    private final class Slot extends AbstractLoadBalanceSlot<Balanced_>
            implements QuadConstraintCollectorValueHandle<A, B, C, D> {

        Slot(DefaultLoadBalance<Balanced_> container) {
            super(container);
        }

        @Override
        public void add(A a, B b, C c, D d) {
            addMapped(balancedItemFunction.apply(a, b, c, d), loadFunction.applyAsLong(a, b, c, d),
                    initialLoadFunction.applyAsLong(a, b, c, d));
        }

        @Override
        public void update(A a, B b, C c, D d) {
            updateMapped(balancedItemFunction.apply(a, b, c, d), loadFunction.applyAsLong(a, b, c, d),
                    initialLoadFunction.applyAsLong(a, b, c, d));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
