package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.impl.util.Triple;

final class ComposeThreeBiCollector<A, B, ResultHolder1_, ResultHolder2_, ResultHolder3_, Result1_, Result2_, Result3_, Result_>
        implements BiConstraintCollector<A, B, Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_>, Result_> {
    private final BiConstraintCollector<A, B, ResultHolder1_, Result1_> first;
    private final BiConstraintCollector<A, B, ResultHolder2_, Result2_> second;
    private final BiConstraintCollector<A, B, ResultHolder3_, Result3_> third;
    private final TriFunction<Result1_, Result2_, Result3_, Result_> composeFunction;

    private final Supplier<ResultHolder1_> firstSupplier;
    private final Supplier<ResultHolder2_> secondSupplier;
    private final Supplier<ResultHolder3_> thirdSupplier;

    private final TriFunction<ResultHolder1_, A, B, Runnable> firstAccumulator;
    private final TriFunction<ResultHolder2_, A, B, Runnable> secondAccumulator;
    private final TriFunction<ResultHolder3_, A, B, Runnable> thirdAccumulator;

    private final Function<ResultHolder1_, Result1_> firstFinisher;
    private final Function<ResultHolder2_, Result2_> secondFinisher;
    private final Function<ResultHolder3_, Result3_> thirdFinisher;

    ComposeThreeBiCollector(BiConstraintCollector<A, B, ResultHolder1_, Result1_> first,
            BiConstraintCollector<A, B, ResultHolder2_, Result2_> second,
            BiConstraintCollector<A, B, ResultHolder3_, Result3_> third,
            TriFunction<Result1_, Result2_, Result3_, Result_> composeFunction) {
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
        return () -> {
            ResultHolder1_ a = firstSupplier.get();
            ResultHolder2_ b = secondSupplier.get();
            return new Triple<>(a, b, thirdSupplier.get());
        };
    }

    @Override
    public TriFunction<Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_>, A, B, Runnable> accumulator() {
        return (resultHolder, a, b) -> composeUndo(firstAccumulator.apply(resultHolder.a(), a, b),
                secondAccumulator.apply(resultHolder.b(), a, b),
                thirdAccumulator.apply(resultHolder.c(), a, b));
    }

    private static Runnable composeUndo(Runnable first, Runnable second, Runnable third) {
        return () -> {
            first.run();
            second.run();
            third.run();
        };
    }

    @Override
    public Function<Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_>, Result_> finisher() {
        return resultHolder -> composeFunction.apply(firstFinisher.apply(resultHolder.a()),
                secondFinisher.apply(resultHolder.b()),
                thirdFinisher.apply(resultHolder.c()));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (ComposeThreeBiCollector<?, ?, ?, ?, ?, ?, ?, ?, ?>) object;
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
