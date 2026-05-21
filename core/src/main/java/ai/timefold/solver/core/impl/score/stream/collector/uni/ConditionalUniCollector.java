package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorValueHandle;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class ConditionalUniCollector<A, ResultContainer_, Result_>
        implements UniConstraintCollector<A, ResultContainer_, Result_> {
    private final Predicate<A> predicate;
    private final UniConstraintCollector<A, ResultContainer_, Result_> delegate;
    private final UniConstraintCollectorAccumulator<ResultContainer_, A> innerIncremental;

    ConditionalUniCollector(Predicate<A> predicate, UniConstraintCollector<A, ResultContainer_, Result_> delegate) {
        this.predicate = predicate;
        this.delegate = delegate;
        this.innerIncremental = UniCollectorUtils.toIncremental(delegate.accumulator());
    }

    @Override
    public @NonNull Supplier<ResultContainer_> supplier() {
        return delegate.supplier();
    }

    @Override
    public @NonNull UniConstraintCollectorAccumulator<ResultContainer_, A> accumulator() {
        return ValueHandle::new;
    }

    @Override
    public @NonNull Function<ResultContainer_, @Nullable Result_> finisher() {
        return delegate.finisher();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (ConditionalUniCollector<?, ?, ?>) object;
        return Objects.equals(predicate, that.predicate) && Objects.equals(delegate, that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicate, delegate);
    }

    private final class ValueHandle implements UniConstraintCollectorValueHandle<A> {
        private final UniConstraintCollectorValueHandle<A> innerValue;
        private boolean active = false;

        ValueHandle(ResultContainer_ container) {
            this.innerValue = innerIncremental.intoGroup(container);
        }

        @Override
        public void add(A a) {
            if (!predicate.test(a)) {
                return;
            }
            active = true;
            innerValue.add(a);
        }

        @Override
        public void replaceWith(A a) {
            var nowActive = predicate.test(a);
            if (active && nowActive) {
                innerValue.replaceWith(a);
            } else if (active) {
                active = false;
                innerValue.remove();
            } else if (nowActive) {
                active = true;
                innerValue.add(a);
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
