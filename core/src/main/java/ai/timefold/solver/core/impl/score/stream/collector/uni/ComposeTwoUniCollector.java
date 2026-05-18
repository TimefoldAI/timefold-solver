package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.util.Pair;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class ComposeTwoUniCollector<A, ResultHolder1_, ResultHolder2_, Result1_, Result2_, Result_>
        implements UniConstraintCollector<A, Pair<ResultHolder1_, ResultHolder2_>, Result_> {
    private final UniConstraintCollector<A, ResultHolder1_, Result1_> first;
    private final UniConstraintCollector<A, ResultHolder2_, Result2_> second;
    private final BiFunction<Result1_, Result2_, Result_> composeFunction;

    private final Supplier<ResultHolder1_> firstSupplier;
    private final Supplier<ResultHolder2_> secondSupplier;

    private final UniConstraintCollectorAccumulator<ResultHolder1_, A> firstIncremental;
    private final UniConstraintCollectorAccumulator<ResultHolder2_, A> secondIncremental;

    private final Function<ResultHolder1_, Result1_> firstFinisher;
    private final Function<ResultHolder2_, Result2_> secondFinisher;

    ComposeTwoUniCollector(UniConstraintCollector<A, ResultHolder1_, Result1_> first,
            UniConstraintCollector<A, ResultHolder2_, Result2_> second,
            BiFunction<Result1_, Result2_, Result_> composeFunction) {
        this.first = first;
        this.second = second;
        this.composeFunction = composeFunction;

        this.firstSupplier = first.supplier();
        this.secondSupplier = second.supplier();

        this.firstIncremental = UniCollectorUtils.toIncremental(first.accumulator());
        this.secondIncremental = UniCollectorUtils.toIncremental(second.accumulator());

        this.firstFinisher = first.finisher();
        this.secondFinisher = second.finisher();
    }

    @Override
    public @NonNull Supplier<Pair<ResultHolder1_, ResultHolder2_>> supplier() {
        return () -> new Pair<>(firstSupplier.get(), secondSupplier.get());
    }

    @Override
    public @NonNull UniConstraintCollectorAccumulator<Pair<ResultHolder1_, ResultHolder2_>, A> accumulator() {
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
        var that = (ComposeTwoUniCollector<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(first, that.first) && Objects.equals(second,
                that.second) && Objects.equals(composeFunction, that.composeFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, composeFunction);
    }

    private final class ValueHandle implements UniConstraintCollectorValueHandle<A> {
        private final UniConstraintCollectorValueHandle<A> v1;
        private final UniConstraintCollectorValueHandle<A> v2;

        ValueHandle(Pair<ResultHolder1_, ResultHolder2_> container) {
            this.v1 = firstIncremental.intoGroup(container.key());
            this.v2 = secondIncremental.intoGroup(container.value());
        }

        @Override
        public void add(A a) {
            v1.add(a);
            v2.add(a);
        }

        @Override
        public void replaceWith(A a) {
            v1.replaceWith(a);
            v2.replaceWith(a);
        }

        @Override
        public void remove() {
            v1.remove();
            v2.remove();
        }
    }
}
