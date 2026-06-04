package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.collector;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.UniNeighborhoodsCollector;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.UniNeighborhoodsCollectorAccumulator;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.UniNeighborhoodsCollectorValueHandle;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ComposeTwoUniNeighborhoodsCollector<Solution_, A, ResultHolder1_, ResultHolder2_, Result1_, Result2_, Result_>
        implements UniNeighborhoodsCollector<Solution_, A, Pair<ResultHolder1_, ResultHolder2_>, Result_> {

    private final UniNeighborhoodsCollector<Solution_, A, ResultHolder1_, Result1_> first;
    private final UniNeighborhoodsCollector<Solution_, A, ResultHolder2_, Result2_> second;
    private final BiFunction<@Nullable Result1_, @Nullable Result2_, @Nullable Result_> composeFunction;

    private final Supplier<ResultHolder1_> firstSupplier;
    private final Supplier<ResultHolder2_> secondSupplier;
    private final UniNeighborhoodsCollectorAccumulator<Solution_, A, ResultHolder1_> firstAccumulator;
    private final UniNeighborhoodsCollectorAccumulator<Solution_, A, ResultHolder2_> secondAccumulator;
    private final Function<ResultHolder1_, @Nullable Result1_> firstFinisher;
    private final Function<ResultHolder2_, @Nullable Result2_> secondFinisher;

    public ComposeTwoUniNeighborhoodsCollector(
            UniNeighborhoodsCollector<Solution_, A, ResultHolder1_, Result1_> first,
            UniNeighborhoodsCollector<Solution_, A, ResultHolder2_, Result2_> second,
            BiFunction<Result1_, Result2_, Result_> composeFunction) {
        this.first = first;
        this.second = second;
        this.composeFunction = composeFunction;
        this.firstSupplier = first.supplier();
        this.secondSupplier = second.supplier();
        this.firstAccumulator = first.accumulator();
        this.secondAccumulator = second.accumulator();
        this.firstFinisher = first.finisher();
        this.secondFinisher = second.finisher();
    }

    @Override
    public Supplier<Pair<ResultHolder1_, ResultHolder2_>> supplier() {
        return () -> new Pair<>(firstSupplier.get(), secondSupplier.get());
    }

    @Override
    public UniNeighborhoodsCollectorAccumulator<Solution_, A, Pair<ResultHolder1_, ResultHolder2_>> accumulator() {
        return ValueHandle::new;
    }

    @Override
    public Function<Pair<ResultHolder1_, ResultHolder2_>, @Nullable Result_> finisher() {
        return pair -> composeFunction.apply(firstFinisher.apply(pair.key()), secondFinisher.apply(pair.value()));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        return object instanceof ComposeTwoUniNeighborhoodsCollector<?, ?, ?, ?, ?, ?, ?> other
                && Objects.equals(first, other.first)
                && Objects.equals(second, other.second)
                && Objects.equals(composeFunction, other.composeFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, composeFunction);
    }

    private final class ValueHandle implements UniNeighborhoodsCollectorValueHandle<A> {

        private final UniNeighborhoodsCollectorValueHandle<A> v1;
        private final UniNeighborhoodsCollectorValueHandle<A> v2;

        ValueHandle(SolutionView<Solution_> view, Pair<ResultHolder1_, ResultHolder2_> container) {
            this.v1 = firstAccumulator.intoGroup(view, container.key());
            this.v2 = secondAccumulator.intoGroup(view, container.value());
        }

        @Override
        public void add(@Nullable A a) {
            v1.add(a);
            v2.add(a);
        }

        @Override
        public void replaceWith(@Nullable A a) {
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
