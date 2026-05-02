package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorAccumulator;
import ai.timefold.solver.core.impl.util.Quadruple;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class ComposeFourTriCollector<A, B, C, ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_, Result1_, Result2_, Result3_, Result4_, Result_>
        implements
        TriConstraintCollector<A, B, C, Quadruple<ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_>, Result_> {
    private final TriConstraintCollector<A, B, C, ResultHolder1_, Result1_> first;
    private final TriConstraintCollector<A, B, C, ResultHolder2_, Result2_> second;
    private final TriConstraintCollector<A, B, C, ResultHolder3_, Result3_> third;
    private final TriConstraintCollector<A, B, C, ResultHolder4_, Result4_> fourth;
    private final QuadFunction<Result1_, Result2_, Result3_, Result4_, Result_> composeFunction;

    private final Supplier<ResultHolder1_> firstSupplier;
    private final Supplier<ResultHolder2_> secondSupplier;
    private final Supplier<ResultHolder3_> thirdSupplier;
    private final Supplier<ResultHolder4_> fourthSupplier;

    private final TriConstraintCollectorAccumulator<ResultHolder1_, A, B, C> firstIncremental;
    private final TriConstraintCollectorAccumulator<ResultHolder2_, A, B, C> secondIncremental;
    private final TriConstraintCollectorAccumulator<ResultHolder3_, A, B, C> thirdIncremental;
    private final TriConstraintCollectorAccumulator<ResultHolder4_, A, B, C> fourthIncremental;

    private final Function<ResultHolder1_, Result1_> firstFinisher;
    private final Function<ResultHolder2_, Result2_> secondFinisher;
    private final Function<ResultHolder3_, Result3_> thirdFinisher;
    private final Function<ResultHolder4_, Result4_> fourthFinisher;

    ComposeFourTriCollector(TriConstraintCollector<A, B, C, ResultHolder1_, Result1_> first,
            TriConstraintCollector<A, B, C, ResultHolder2_, Result2_> second,
            TriConstraintCollector<A, B, C, ResultHolder3_, Result3_> third,
            TriConstraintCollector<A, B, C, ResultHolder4_, Result4_> fourth,
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

        this.firstIncremental =
                first.isIncremental() ? first.incrementalAccumulator() : TriCollectorUtils.toIncremental(first.accumulator());
        this.secondIncremental = second.isIncremental() ? second.incrementalAccumulator()
                : TriCollectorUtils.toIncremental(second.accumulator());
        this.thirdIncremental =
                third.isIncremental() ? third.incrementalAccumulator() : TriCollectorUtils.toIncremental(third.accumulator());
        this.fourthIncremental = fourth.isIncremental() ? fourth.incrementalAccumulator()
                : TriCollectorUtils.toIncremental(fourth.accumulator());

        this.firstFinisher = first.finisher();
        this.secondFinisher = second.finisher();
        this.thirdFinisher = third.finisher();
        this.fourthFinisher = fourth.finisher();
    }

    @Override
    public @NonNull Supplier<Quadruple<ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_>> supplier() {
        return () -> {
            ResultHolder1_ a = firstSupplier.get();
            ResultHolder2_ b = secondSupplier.get();
            ResultHolder3_ c = thirdSupplier.get();
            return new Quadruple<>(a, b, c, fourthSupplier.get());
        };
    }

    @Override
    public @NonNull QuadFunction<Quadruple<ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_>, A, B, C, Runnable>
            accumulator() {
        return TriCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull
            TriConstraintCollectorAccumulator<Quadruple<ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_>, A, B, C>
            incrementalAccumulator() {
        return AccumulatedValue::new;
    }

    @Override
    public @NonNull Function<Quadruple<ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_>, @Nullable Result_>
            finisher() {
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
        var that = (ComposeFourTriCollector<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) object;
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

    private final class AccumulatedValue implements TriConstraintCollectorAccumulatedValue<A, B, C> {
        private final TriConstraintCollectorAccumulatedValue<A, B, C> v1;
        private final TriConstraintCollectorAccumulatedValue<A, B, C> v2;
        private final TriConstraintCollectorAccumulatedValue<A, B, C> v3;
        private final TriConstraintCollectorAccumulatedValue<A, B, C> v4;

        AccumulatedValue(Quadruple<ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_> container) {
            this.v1 = firstIncremental.intoGroup(container.a());
            this.v2 = secondIncremental.intoGroup(container.b());
            this.v3 = thirdIncremental.intoGroup(container.c());
            this.v4 = fourthIncremental.intoGroup(container.d());
        }

        @Override
        public void add(A a, B b, C c) {
            v1.add(a, b, c);
            v2.add(a, b, c);
            v3.add(a, b, c);
            v4.add(a, b, c);
        }

        @Override
        public void update(A a, B b, C c) {
            v1.update(a, b, c);
            v2.update(a, b, c);
            v3.update(a, b, c);
            v4.update(a, b, c);
        }

        @Override
        public void remove() {
            v1.remove();
            v2.remove();
            v3.remove();
            v4.remove();
        }
    }
}
