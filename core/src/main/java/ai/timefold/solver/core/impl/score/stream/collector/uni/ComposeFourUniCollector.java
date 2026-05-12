package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.util.Quadruple;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class ComposeFourUniCollector<A, ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_, Result1_, Result2_, Result3_, Result4_, Result_>
        implements
        UniConstraintCollector<A, Quadruple<ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_>, Result_> {
    private final UniConstraintCollector<A, ResultHolder1_, Result1_> first;
    private final UniConstraintCollector<A, ResultHolder2_, Result2_> second;
    private final UniConstraintCollector<A, ResultHolder3_, Result3_> third;
    private final UniConstraintCollector<A, ResultHolder4_, Result4_> fourth;
    private final QuadFunction<Result1_, Result2_, Result3_, Result4_, Result_> composeFunction;

    private final Supplier<ResultHolder1_> firstSupplier;
    private final Supplier<ResultHolder2_> secondSupplier;
    private final Supplier<ResultHolder3_> thirdSupplier;
    private final Supplier<ResultHolder4_> fourthSupplier;

    private final UniConstraintCollectorAccumulator<ResultHolder1_, A> firstIncremental;
    private final UniConstraintCollectorAccumulator<ResultHolder2_, A> secondIncremental;
    private final UniConstraintCollectorAccumulator<ResultHolder3_, A> thirdIncremental;
    private final UniConstraintCollectorAccumulator<ResultHolder4_, A> fourthIncremental;

    private final Function<ResultHolder1_, Result1_> firstFinisher;
    private final Function<ResultHolder2_, Result2_> secondFinisher;
    private final Function<ResultHolder3_, Result3_> thirdFinisher;
    private final Function<ResultHolder4_, Result4_> fourthFinisher;

    ComposeFourUniCollector(UniConstraintCollector<A, ResultHolder1_, Result1_> first,
            UniConstraintCollector<A, ResultHolder2_, Result2_> second,
            UniConstraintCollector<A, ResultHolder3_, Result3_> third,
            UniConstraintCollector<A, ResultHolder4_, Result4_> fourth,
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
                first.isIncremental() ? first.incrementalAccumulator() : UniCollectorUtils.toIncremental(first.accumulator());
        this.secondIncremental = second.isIncremental() ? second.incrementalAccumulator()
                : UniCollectorUtils.toIncremental(second.accumulator());
        this.thirdIncremental =
                third.isIncremental() ? third.incrementalAccumulator() : UniCollectorUtils.toIncremental(third.accumulator());
        this.fourthIncremental = fourth.isIncremental() ? fourth.incrementalAccumulator()
                : UniCollectorUtils.toIncremental(fourth.accumulator());

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
    public @NonNull BiFunction<Quadruple<ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_>, A, Runnable>
            accumulator() {
        return UniCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull
            UniConstraintCollectorAccumulator<Quadruple<ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_>, A>
            incrementalAccumulator() {
        return ValueHandle::new;
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
        var that = (ComposeFourUniCollector<?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) object;
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

    private final class ValueHandle implements UniConstraintCollectorValueHandle<A> {
        private final UniConstraintCollectorValueHandle<A> v1;
        private final UniConstraintCollectorValueHandle<A> v2;
        private final UniConstraintCollectorValueHandle<A> v3;
        private final UniConstraintCollectorValueHandle<A> v4;

        ValueHandle(Quadruple<ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_> container) {
            this.v1 = firstIncremental.intoGroup(container.a());
            this.v2 = secondIncremental.intoGroup(container.b());
            this.v3 = thirdIncremental.intoGroup(container.c());
            this.v4 = fourthIncremental.intoGroup(container.d());
        }

        @Override
        public void add(A a) {
            v1.add(a);
            v2.add(a);
            v3.add(a);
            v4.add(a);
        }

        @Override
        public void update(A a) {
            v1.update(a);
            v2.update(a);
            v3.update(a);
            v4.update(a);
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
