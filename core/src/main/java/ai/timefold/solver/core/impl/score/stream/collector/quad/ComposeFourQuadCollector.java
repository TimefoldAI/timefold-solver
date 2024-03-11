package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.impl.util.Quadruple;

final class ComposeFourQuadCollector<A, B, C, D, ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_, Result1_, Result2_, Result3_, Result4_, Result_>
        implements
        QuadConstraintCollector<A, B, C, D, Quadruple<ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_>, Result_> {
    private final QuadConstraintCollector<A, B, C, D, ResultHolder1_, Result1_> first;
    private final QuadConstraintCollector<A, B, C, D, ResultHolder2_, Result2_> second;
    private final QuadConstraintCollector<A, B, C, D, ResultHolder3_, Result3_> third;
    private final QuadConstraintCollector<A, B, C, D, ResultHolder4_, Result4_> fourth;
    private final QuadFunction<Result1_, Result2_, Result3_, Result4_, Result_> composeFunction;

    private final Supplier<ResultHolder1_> firstSupplier;
    private final Supplier<ResultHolder2_> secondSupplier;
    private final Supplier<ResultHolder3_> thirdSupplier;
    private final Supplier<ResultHolder4_> fourthSupplier;

    private final PentaFunction<ResultHolder1_, A, B, C, D, Runnable> firstAccumulator;
    private final PentaFunction<ResultHolder2_, A, B, C, D, Runnable> secondAccumulator;
    private final PentaFunction<ResultHolder3_, A, B, C, D, Runnable> thirdAccumulator;
    private final PentaFunction<ResultHolder4_, A, B, C, D, Runnable> fourthAccumulator;

    private final Function<ResultHolder1_, Result1_> firstFinisher;
    private final Function<ResultHolder2_, Result2_> secondFinisher;
    private final Function<ResultHolder3_, Result3_> thirdFinisher;
    private final Function<ResultHolder4_, Result4_> fourthFinisher;

    ComposeFourQuadCollector(QuadConstraintCollector<A, B, C, D, ResultHolder1_, Result1_> first,
            QuadConstraintCollector<A, B, C, D, ResultHolder2_, Result2_> second,
            QuadConstraintCollector<A, B, C, D, ResultHolder3_, Result3_> third,
            QuadConstraintCollector<A, B, C, D, ResultHolder4_, Result4_> fourth,
            QuadFunction<Result1_, Result2_, Result3_, Result4_, Result_> composeFunction) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
        this.composeFunction = composeFunction;

        this.firstSupplier = first.supplier();
        this.secondSupplier = second.supplier();
        this.thirdSupplier = third.supplier();
        this.fourthSupplier = fourth.supplier();

        this.firstAccumulator = first.accumulator();
        this.secondAccumulator = second.accumulator();
        this.thirdAccumulator = third.accumulator();
        this.fourthAccumulator = fourth.accumulator();

        this.firstFinisher = first.finisher();
        this.secondFinisher = second.finisher();
        this.thirdFinisher = third.finisher();
        this.fourthFinisher = fourth.finisher();
    }

    @Override
    public Supplier<Quadruple<ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_>> supplier() {
        return () -> {
            ResultHolder1_ a = firstSupplier.get();
            ResultHolder2_ b = secondSupplier.get();
            ResultHolder3_ c = thirdSupplier.get();
            return new Quadruple<>(a, b, c, fourthSupplier.get());
        };
    }

    @Override
    public PentaFunction<Quadruple<ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_>, A, B, C, D, Runnable>
            accumulator() {
        return (resultHolder, a, b, c, d) -> composeUndo(firstAccumulator.apply(resultHolder.a(), a, b, c, d),
                secondAccumulator.apply(resultHolder.b(), a, b, c, d),
                thirdAccumulator.apply(resultHolder.c(), a, b, c, d),
                fourthAccumulator.apply(resultHolder.d(), a, b, c, d));
    }

    private static Runnable composeUndo(Runnable first, Runnable second, Runnable third,
            Runnable fourth) {
        return () -> {
            first.run();
            second.run();
            third.run();
            fourth.run();
        };
    }

    @Override
    public Function<Quadruple<ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_>, Result_> finisher() {
        return resultHolder -> composeFunction.apply(firstFinisher.apply(resultHolder.a()),
                secondFinisher.apply(resultHolder.b()),
                thirdFinisher.apply(resultHolder.c()),
                fourthFinisher.apply(resultHolder.d()));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (ComposeFourQuadCollector<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(first, that.first) && Objects.equals(second,
                that.second) && Objects.equals(third, that.third)
                && Objects.equals(fourth,
                        that.fourth)
                && Objects.equals(composeFunction, that.composeFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, third, fourth, composeFunction);
    }
}
