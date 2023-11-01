package ai.timefold.solver.core.impl.score.stream.tri;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.impl.util.Triple;

public final class TriComposeThreeCollector<A, B, C, ResultHolder1_, ResultHolder2_, ResultHolder3_, Result1_, Result2_, Result3_, Result>
        implements TriConstraintCollector<A, B, C, Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_>, Result> {
    private final TriConstraintCollector<A, B, C, ResultHolder1_, Result1_> first;
    private final TriConstraintCollector<A, B, C, ResultHolder2_, Result2_> second;
    private final TriConstraintCollector<A, B, C, ResultHolder3_, Result3_> third;
    private final TriFunction<Result1_, Result2_, Result3_, Result> composeFunction;

    private final Supplier<ResultHolder1_> firstSupplier;
    private final Supplier<ResultHolder2_> secondSupplier;
    private final Supplier<ResultHolder3_> thirdSupplier;

    private final QuadFunction<ResultHolder1_, A, B, C, Runnable> firstAccumulator;
    private final QuadFunction<ResultHolder2_, A, B, C, Runnable> secondAccumulator;
    private final QuadFunction<ResultHolder3_, A, B, C, Runnable> thirdAccumulator;

    private final Function<ResultHolder1_, Result1_> firstFinisher;
    private final Function<ResultHolder2_, Result2_> secondFinisher;
    private final Function<ResultHolder3_, Result3_> thirdFinisher;

    public TriComposeThreeCollector(TriConstraintCollector<A, B, C, ResultHolder1_, Result1_> first,
            TriConstraintCollector<A, B, C, ResultHolder2_, Result2_> second,
            TriConstraintCollector<A, B, C, ResultHolder3_, Result3_> third,
            TriFunction<Result1_, Result2_, Result3_, Result> composeFunction) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.composeFunction = composeFunction;

        this.firstSupplier = first.supplier();
        this.secondSupplier = second.supplier();
        this.thirdSupplier = third.supplier();

        this.firstAccumulator = first.accumulator();
        this.secondAccumulator = second.accumulator();
        this.thirdAccumulator = third.accumulator();

        this.firstFinisher = first.finisher();
        this.secondFinisher = second.finisher();
        this.thirdFinisher = third.finisher();
    }

    @Override
    public Supplier<Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_>> supplier() {
        return () -> Triple.of(firstSupplier.get(), secondSupplier.get(), thirdSupplier.get());
    }

    @Override
    public QuadFunction<Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_>, A, B, C, Runnable> accumulator() {
        return (resultHolder, a, b, c) -> composeUndo(firstAccumulator.apply(resultHolder.getA(), a, b, c),
                secondAccumulator.apply(resultHolder.getB(), a, b, c),
                thirdAccumulator.apply(resultHolder.getC(), a, b, c));
    }

    private static Runnable composeUndo(Runnable first, Runnable second, Runnable third) {
        return () -> {
            first.run();
            second.run();
            third.run();
        };
    }

    @Override
    public Function<Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_>, Result> finisher() {
        return resultHolder -> composeFunction.apply(firstFinisher.apply(resultHolder.getA()),
                secondFinisher.apply(resultHolder.getB()),
                thirdFinisher.apply(resultHolder.getC()));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        TriComposeThreeCollector<?, ?, ?, ?, ?, ?, ?, ?, ?, ?> that =
                (TriComposeThreeCollector<?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(first, that.first) && Objects.equals(second,
                that.second) && Objects.equals(third, that.third)
                && Objects.equals(composeFunction,
                        that.composeFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, third, composeFunction);
    }
}
