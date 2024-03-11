package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.impl.util.Pair;

final class ComposeTwoTriCollector<A, B, C, ResultHolder1_, ResultHolder2_, Result1_, Result2_, Result_>
        implements TriConstraintCollector<A, B, C, Pair<ResultHolder1_, ResultHolder2_>, Result_> {
    private final TriConstraintCollector<A, B, C, ResultHolder1_, Result1_> first;
    private final TriConstraintCollector<A, B, C, ResultHolder2_, Result2_> second;
    private final BiFunction<Result1_, Result2_, Result_> composeFunction;

    private final Supplier<ResultHolder1_> firstSupplier;
    private final Supplier<ResultHolder2_> secondSupplier;

    private final QuadFunction<ResultHolder1_, A, B, C, Runnable> firstAccumulator;
    private final QuadFunction<ResultHolder2_, A, B, C, Runnable> secondAccumulator;

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
    public QuadFunction<Pair<ResultHolder1_, ResultHolder2_>, A, B, C, Runnable> accumulator() {
        return (resultHolder, a, b, c) -> composeUndo(firstAccumulator.apply(resultHolder.key(), a, b, c),
                secondAccumulator.apply(resultHolder.value(), a, b, c));
    }

    private static Runnable composeUndo(Runnable first, Runnable second) {
        return () -> {
            first.run();
            second.run();
        };
    }

    @Override
    public Function<Pair<ResultHolder1_, ResultHolder2_>, Result_> finisher() {
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
}
