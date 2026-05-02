package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorAccumulator;

import org.jspecify.annotations.NonNull;

final class ConditionalQuadCollector<A, B, C, D, ResultContainer_, Result_>
        implements QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_> {
    private final QuadPredicate<A, B, C, D> predicate;
    private final QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_> delegate;
    private final QuadConstraintCollectorAccumulator<ResultContainer_, A, B, C, D> innerIncremental;

    ConditionalQuadCollector(QuadPredicate<A, B, C, D> predicate,
            QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_> delegate) {
        this.predicate = predicate;
        this.delegate = delegate;
        this.innerIncremental = delegate.isIncremental() ? delegate.incrementalAccumulator()
                : QuadCollectorUtils.toIncremental(delegate.accumulator());
    }

    @Override
    public @NonNull Supplier<ResultContainer_> supplier() {
        return delegate.supplier();
    }

    @Override
    public @NonNull PentaFunction<ResultContainer_, A, B, C, D, Runnable> accumulator() {
        return QuadCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull QuadConstraintCollectorAccumulator<ResultContainer_, A, B, C, D> incrementalAccumulator() {
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
        var that = (ConditionalQuadCollector<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(predicate, that.predicate) && Objects.equals(delegate, that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicate, delegate);
    }

    private final class AccumulatedValue implements QuadConstraintCollectorAccumulatedValue<A, B, C, D> {
        private final QuadConstraintCollectorAccumulatedValue<A, B, C, D> innerValue;
        private boolean active = false;

        AccumulatedValue(ResultContainer_ container) {
            this.innerValue = innerIncremental.intoGroup(container);
        }

        @Override
        public void add(A a, B b, C c, D d) {
            if (!predicate.test(a, b, c, d)) {
                return;
            }
            active = true;
            innerValue.add(a, b, c, d);
        }

        @Override
        public void update(A a, B b, C c, D d) {
            boolean nowActive = predicate.test(a, b, c, d);
            if (active && nowActive) {
                innerValue.update(a, b, c, d);
            } else if (active) {
                active = false;
                innerValue.remove();
            } else if (nowActive) {
                active = true;
                innerValue.add(a, b, c, d);
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
