package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulator;

import org.jspecify.annotations.NonNull;

final class ConditionalBiCollector<A, B, ResultContainer_, Result_>
        implements BiConstraintCollector<A, B, ResultContainer_, Result_> {
    private final BiPredicate<A, B> predicate;
    private final BiConstraintCollector<A, B, ResultContainer_, Result_> delegate;
    private final BiConstraintCollectorAccumulator<ResultContainer_, A, B> innerIncremental;

    ConditionalBiCollector(BiPredicate<A, B> predicate,
            BiConstraintCollector<A, B, ResultContainer_, Result_> delegate) {
        this.predicate = predicate;
        this.delegate = delegate;
        this.innerIncremental = delegate.isIncremental() ? delegate.incrementalAccumulator()
                : BiCollectorUtils.toIncremental(delegate.accumulator());
    }

    @Override
    public @NonNull Supplier<ResultContainer_> supplier() {
        return delegate.supplier();
    }

    @Override
    public @NonNull TriFunction<ResultContainer_, A, B, Runnable> accumulator() {
        return BiCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull BiConstraintCollectorAccumulator<ResultContainer_, A, B> incrementalAccumulator() {
        return AccumulatedValue::new;
    }

    @Override
    public @NonNull Function<ResultContainer_, Result_> finisher() {
        return delegate.finisher();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (ConditionalBiCollector<?, ?, ?, ?>) object;
        return Objects.equals(predicate, that.predicate) && Objects.equals(delegate, that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicate, delegate);
    }

    private final class AccumulatedValue implements BiConstraintCollectorAccumulatedValue<A, B> {
        private final BiConstraintCollectorAccumulatedValue<A, B> innerValue;
        private boolean active = false;

        AccumulatedValue(ResultContainer_ container) {
            this.innerValue = innerIncremental.intoGroup(container);
        }

        @Override
        public void add(A a, B b) {
            if (!predicate.test(a, b)) {
                return;
            }
            active = true;
            innerValue.add(a, b);
        }

        @Override
        public void update(A a, B b) {
            var nowActive = predicate.test(a, b);
            if (active && nowActive) {
                innerValue.update(a, b);
            } else if (active) {
                active = false;
                innerValue.remove();
            } else if (nowActive) {
                active = true;
                innerValue.add(a, b);
            }
        }

        @Override
        public void remove() {
            if (!active) {
                return;
            }
            active = false;
            innerValue.remove();
        }
    }
}
