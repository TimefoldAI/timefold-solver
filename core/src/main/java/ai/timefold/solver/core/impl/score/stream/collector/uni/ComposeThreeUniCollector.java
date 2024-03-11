package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.util.Triple;

final class ComposeThreeUniCollector<A, ResultHolder1_, ResultHolder2_, ResultHolder3_, Result1_, Result2_, Result3_, Result_>
        implements UniConstraintCollector<A, Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_>, Result_> {
    private final UniConstraintCollector<A, ResultHolder1_, Result1_> first;
    private final UniConstraintCollector<A, ResultHolder2_, Result2_> second;
    private final UniConstraintCollector<A, ResultHolder3_, Result3_> third;
    private final TriFunction<Result1_, Result2_, Result3_, Result_> composeFunction;

    private final Supplier<ResultHolder1_> firstSupplier;
    private final Supplier<ResultHolder2_> secondSupplier;
    private final Supplier<ResultHolder3_> thirdSupplier;

    private final BiFunction<ResultHolder1_, A, Runnable> firstAccumulator;
    private final BiFunction<ResultHolder2_, A, Runnable> secondAccumulator;
    private final BiFunction<ResultHolder3_, A, Runnable> thirdAccumulator;

    private final Function<ResultHolder1_, Result1_> firstFinisher;
    private final Function<ResultHolder2_, Result2_> secondFinisher;
    private final Function<ResultHolder3_, Result3_> thirdFinisher;

    ComposeThreeUniCollector(UniConstraintCollector<A, ResultHolder1_, Result1_> first,
            UniConstraintCollector<A, ResultHolder2_, Result2_> second,
            UniConstraintCollector<A, ResultHolder3_, Result3_> third,
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
    public BiFunction<Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_>, A, Runnable> accumulator() {
        return (resultHolder, a) -> composeUndo(firstAccumulator.apply(resultHolder.a(), a),
                secondAccumulator.apply(resultHolder.b(), a),
                thirdAccumulator.apply(resultHolder.c(), a));
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
        var that = (ComposeThreeUniCollector<?, ?, ?, ?, ?, ?, ?, ?>) object;
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
