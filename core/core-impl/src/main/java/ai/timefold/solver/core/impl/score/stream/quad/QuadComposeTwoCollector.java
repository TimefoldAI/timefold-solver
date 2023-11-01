package ai.timefold.solver.core.impl.score.stream.quad;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.impl.util.Pair;

public final class QuadComposeTwoCollector<A, B, C, D, ResultHolder1_, ResultHolder2_, Result1_, Result2_, Result>
        implements QuadConstraintCollector<A, B, C, D, Pair<ResultHolder1_, ResultHolder2_>, Result> {
    private final QuadConstraintCollector<A, B, C, D, ResultHolder1_, Result1_> first;
    private final QuadConstraintCollector<A, B, C, D, ResultHolder2_, Result2_> second;
    private final BiFunction<Result1_, Result2_, Result> composeFunction;

    private final Supplier<ResultHolder1_> firstSupplier;
    private final Supplier<ResultHolder2_> secondSupplier;

    private final PentaFunction<ResultHolder1_, A, B, C, D, Runnable> firstAccumulator;
    private final PentaFunction<ResultHolder2_, A, B, C, D, Runnable> secondAccumulator;

    private final Function<ResultHolder1_, Result1_> firstFinisher;
    private final Function<ResultHolder2_, Result2_> secondFinisher;

    public QuadComposeTwoCollector(QuadConstraintCollector<A, B, C, D, ResultHolder1_, Result1_> first,
            QuadConstraintCollector<A, B, C, D, ResultHolder2_, Result2_> second,
            BiFunction<Result1_, Result2_, Result> composeFunction) {
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
        return () -> Pair.of(firstSupplier.get(), secondSupplier.get());
    }

    @Override
    public PentaFunction<Pair<ResultHolder1_, ResultHolder2_>, A, B, C, D, Runnable> accumulator() {
        return (resultHolder, a, b, c, d) -> composeUndo(firstAccumulator.apply(resultHolder.getKey(), a, b, c, d),
                secondAccumulator.apply(resultHolder.getValue(), a, b, c, d));
    }

    private static Runnable composeUndo(Runnable first, Runnable second) {
        return () -> {
            first.run();
            second.run();
        };
    }

    @Override
    public Function<Pair<ResultHolder1_, ResultHolder2_>, Result> finisher() {
        return resultHolder -> composeFunction.apply(firstFinisher.apply(resultHolder.getKey()),
                secondFinisher.apply(resultHolder.getValue()));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        QuadComposeTwoCollector<?, ?, ?, ?, ?, ?, ?, ?, ?> that = (QuadComposeTwoCollector<?, ?, ?, ?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(first, that.first) && Objects.equals(second,
                that.second) && Objects.equals(composeFunction, that.composeFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, composeFunction);
    }
}
