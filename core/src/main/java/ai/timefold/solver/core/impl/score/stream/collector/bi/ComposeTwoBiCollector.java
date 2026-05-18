package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.util.Pair;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class ComposeTwoBiCollector<A, B, ResultHolder1_, ResultHolder2_, Result1_, Result2_, Result_>
        implements BiConstraintCollector<A, B, Pair<ResultHolder1_, ResultHolder2_>, Result_> {
    private final BiConstraintCollector<A, B, ResultHolder1_, Result1_> first;
    private final BiConstraintCollector<A, B, ResultHolder2_, Result2_> second;
    private final BiFunction<Result1_, Result2_, Result_> composeFunction;

    private final Supplier<ResultHolder1_> firstSupplier;
    private final Supplier<ResultHolder2_> secondSupplier;

    private final BiConstraintCollectorAccumulator<ResultHolder1_, A, B> firstIncremental;
    private final BiConstraintCollectorAccumulator<ResultHolder2_, A, B> secondIncremental;

    private final Function<ResultHolder1_, Result1_> firstFinisher;
    private final Function<ResultHolder2_, Result2_> secondFinisher;

    ComposeTwoBiCollector(BiConstraintCollector<A, B, ResultHolder1_, Result1_> first,
            BiConstraintCollector<A, B, ResultHolder2_, Result2_> second,
            BiFunction<Result1_, Result2_, Result_> composeFunction) {
        this.first = first;
        this.second = second;
        this.composeFunction = composeFunction;

        this.firstSupplier = first.supplier();
        this.secondSupplier = second.supplier();

        this.firstIncremental = BiCollectorUtils.toIncremental(first.accumulator());
        this.secondIncremental = BiCollectorUtils.toIncremental(second.accumulator());

        this.firstFinisher = first.finisher();
        this.secondFinisher = second.finisher();
    }

    @Override
    public @NonNull Supplier<Pair<ResultHolder1_, ResultHolder2_>> supplier() {
        return () -> new Pair<>(firstSupplier.get(), secondSupplier.get());
    }

    @Override
    public @NonNull BiConstraintCollectorAccumulator<Pair<ResultHolder1_, ResultHolder2_>, A, B> accumulator() {
        return ValueHandle::new;
    }

    @Override
    public @NonNull Function<Pair<ResultHolder1_, ResultHolder2_>, @Nullable Result_> finisher() {
        return resultHolder -> composeFunction.apply(firstFinisher.apply(resultHolder.key()),
                secondFinisher.apply(resultHolder.value()));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (ComposeTwoBiCollector<?, ?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(first, that.first) && Objects.equals(second,
                that.second) && Objects.equals(composeFunction, that.composeFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, composeFunction);
    }

    private final class ValueHandle implements BiConstraintCollectorValueHandle<A, B> {
        private final BiConstraintCollectorValueHandle<A, B> v1;
        private final BiConstraintCollectorValueHandle<A, B> v2;

        ValueHandle(Pair<ResultHolder1_, ResultHolder2_> container) {
            this.v1 = firstIncremental.intoGroup(container.key());
            this.v2 = secondIncremental.intoGroup(container.value());
        }

        @Override
        public void add(A a, B b) {
            v1.add(a, b);
            v2.add(a, b);
        }

        @Override
        public void replaceWith(A a, B b) {
            v1.replaceWith(a, b);
            v2.replaceWith(a, b);
        }

        @Override
        public void remove() {
            v1.remove();
            v2.remove();
        }
    }
}
