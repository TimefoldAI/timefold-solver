package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorValueHandle;

import org.jspecify.annotations.NonNull;

final class ConditionalTriCollector<A, B, C, ResultContainer_, Result_>
        implements TriConstraintCollector<A, B, C, ResultContainer_, Result_> {
    private final TriPredicate<A, B, C> predicate;
    private final TriConstraintCollector<A, B, C, ResultContainer_, Result_> delegate;
    private final TriConstraintCollectorAccumulator<ResultContainer_, A, B, C> innerIncremental;

    ConditionalTriCollector(TriPredicate<A, B, C> predicate,
            TriConstraintCollector<A, B, C, ResultContainer_, Result_> delegate) {
        this.predicate = predicate;
        this.delegate = delegate;
        this.innerIncremental = TriCollectorUtils.toIncremental(delegate.accumulator());
    }

    @Override
    public @NonNull Supplier<ResultContainer_> supplier() {
        return delegate.supplier();
    }

    @Override
    public @NonNull TriConstraintCollectorAccumulator<ResultContainer_, A, B, C> accumulator() {
        return ValueHandle::new;
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
        var that = (ConditionalTriCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(predicate, that.predicate) && Objects.equals(delegate, that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicate, delegate);
    }

    private final class ValueHandle implements TriConstraintCollectorValueHandle<A, B, C> {
        private final TriConstraintCollectorValueHandle<A, B, C> innerValue;
        private boolean active = false;

        ValueHandle(ResultContainer_ container) {
            this.innerValue = innerIncremental.intoGroup(container);
        }

        @Override
        public void add(A a, B b, C c) {
            if (!predicate.test(a, b, c)) {
                return;
            }
            active = true;
            innerValue.add(a, b, c);
        }

        @Override
        public void replaceWith(A a, B b, C c) {
            var nowActive = predicate.test(a, b, c);
            if (active && nowActive) {
                innerValue.replaceWith(a, b, c);
            } else if (active) {
                active = false;
                innerValue.remove();
            } else if (nowActive) {
                active = true;
                innerValue.add(a, b, c);
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
