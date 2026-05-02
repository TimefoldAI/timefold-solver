package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;
import ai.timefold.solver.core.impl.util.Triple;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class ComposeThreeUniCollector<A, ResultHolder1_, ResultHolder2_, ResultHolder3_, Result1_, Result2_, Result3_, Result_>
        implements UniConstraintCollector<A, Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_>, Result_> {
    private final UniConstraintCollector<A, ResultHolder1_, Result1_> first;
    private final UniConstraintCollector<A, ResultHolder2_, Result2_> second;
    private final UniConstraintCollector<A, ResultHolder3_, Result3_> third;
    private final TriFunction<Result1_, Result2_, Result3_, Result_> composeFunction;

    private final Supplier<ResultHolder1_> firstSupplier;
    private final Supplier<ResultHolder2_> secondSupplier;
    private final Supplier<ResultHolder3_> thirdSupplier;

    private final UniConstraintCollectorAccumulator<ResultHolder1_, A> firstIncremental;
    private final UniConstraintCollectorAccumulator<ResultHolder2_, A> secondIncremental;
    private final UniConstraintCollectorAccumulator<ResultHolder3_, A> thirdIncremental;

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

        this.firstIncremental =
                first.isIncremental() ? first.incrementalAccumulator() : UniCollectorUtils.toIncremental(first.accumulator());
        this.secondIncremental = second.isIncremental() ? second.incrementalAccumulator()
                : UniCollectorUtils.toIncremental(second.accumulator());
        this.thirdIncremental =
                third.isIncremental() ? third.incrementalAccumulator() : UniCollectorUtils.toIncremental(third.accumulator());

        this.firstFinisher = first.finisher();
        this.secondFinisher = second.finisher();
        this.thirdFinisher = third.finisher();
    }

    @Override
    public @NonNull Supplier<Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_>> supplier() {
        return () -> {
            ResultHolder1_ a = firstSupplier.get();
            ResultHolder2_ b = secondSupplier.get();
            return new Triple<>(a, b, thirdSupplier.get());
        };
    }

    @Override
    public @NonNull BiFunction<Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_>, A, Runnable> accumulator() {
        return UniCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull UniConstraintCollectorAccumulator<Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_>, A>
            incrementalAccumulator() {
        return AccumulatedValue::new;
    }

    @Override
    public @NonNull Function<Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_>, @Nullable Result_> finisher() {
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

    private final class AccumulatedValue implements UniConstraintCollectorAccumulatedValue<A> {
        private final UniConstraintCollectorAccumulatedValue<A> v1;
        private final UniConstraintCollectorAccumulatedValue<A> v2;
        private final UniConstraintCollectorAccumulatedValue<A> v3;

        AccumulatedValue(Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_> container) {
            this.v1 = firstIncremental.intoGroup(container.a());
            this.v2 = secondIncremental.intoGroup(container.b());
            this.v3 = thirdIncremental.intoGroup(container.c());
        }

        @Override
        public void add(A a) {
            v1.add(a);
            v2.add(a);
            v3.add(a);
        }

        @Override
        public void update(A a) {
            v1.update(a);
            v2.update(a);
            v3.update(a);
        }

        @Override
        public void remove() {
            v1.remove();
            v2.remove();
            v3.remove();
        }
    }
}
