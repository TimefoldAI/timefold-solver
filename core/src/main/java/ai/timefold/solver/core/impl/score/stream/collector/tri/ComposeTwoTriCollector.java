package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.util.Pair;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class ComposeTwoTriCollector<A, B, C, ResultHolder1_, ResultHolder2_, Result1_, Result2_, Result_>
        implements TriConstraintCollector<A, B, C, Pair<ResultHolder1_, ResultHolder2_>, Result_> {
    private final TriConstraintCollector<A, B, C, ResultHolder1_, Result1_> first;
    private final TriConstraintCollector<A, B, C, ResultHolder2_, Result2_> second;
    private final BiFunction<Result1_, Result2_, Result_> composeFunction;

    private final Supplier<ResultHolder1_> firstSupplier;
    private final Supplier<ResultHolder2_> secondSupplier;

    private final TriConstraintCollectorAccumulator<ResultHolder1_, A, B, C> firstIncremental;
    private final TriConstraintCollectorAccumulator<ResultHolder2_, A, B, C> secondIncremental;

    private final Function<ResultHolder1_, Result1_> firstFinisher;
    private final Function<ResultHolder2_, Result2_> secondFinisher;

    ComposeTwoTriCollector(TriConstraintCollector<A, B, C, ResultHolder1_, Result1_> first,
            TriConstraintCollector<A, B, C, ResultHolder2_, Result2_> second,
            BiFunction<Result1_, Result2_, Result_> composeFunction) {
        this.first = first;
        this.second = second;
        this.composeFunction = composeFunction;

        this.firstSupplier = first.supplier();
        this.secondSupplier = second.supplier();

        this.firstIncremental = TriCollectorUtils.toIncremental(first.accumulator());
        this.secondIncremental = TriCollectorUtils.toIncremental(second.accumulator());

        this.firstFinisher = first.finisher();
        this.secondFinisher = second.finisher();
    }

    @Override
    public @NonNull Supplier<Pair<ResultHolder1_, ResultHolder2_>> supplier() {
        return () -> new Pair<>(firstSupplier.get(), secondSupplier.get());
    }

    @Override
    public @NonNull TriConstraintCollectorAccumulator<Pair<ResultHolder1_, ResultHolder2_>, A, B, C> accumulator() {
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
        var that = (ComposeTwoTriCollector<?, ?, ?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(first, that.first) && Objects.equals(second,
                that.second) && Objects.equals(composeFunction, that.composeFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, composeFunction);
    }

    private final class ValueHandle implements TriConstraintCollectorValueHandle<A, B, C> {
        private final TriConstraintCollectorValueHandle<A, B, C> v1;
        private final TriConstraintCollectorValueHandle<A, B, C> v2;

        ValueHandle(Pair<ResultHolder1_, ResultHolder2_> container) {
            this.v1 = firstIncremental.intoGroup(container.key());
            this.v2 = secondIncremental.intoGroup(container.value());
        }

        @Override
        public void add(A a, B b, C c) {
            v1.add(a, b, c);
            v2.add(a, b, c);
        }

        @Override
        public void replaceWith(A a, B b, C c) {
            v1.replaceWith(a, b, c);
            v2.replaceWith(a, b, c);
        }

        @Override
        public void remove() {
            v1.remove();
            v2.remove();
        }
    }
}
