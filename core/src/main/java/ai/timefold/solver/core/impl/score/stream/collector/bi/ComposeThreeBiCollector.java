package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.util.Triple;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class ComposeThreeBiCollector<A, B, ResultHolder1_, ResultHolder2_, ResultHolder3_, Result1_, Result2_, Result3_, Result_>
        implements BiConstraintCollector<A, B, Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_>, Result_> {
    private final BiConstraintCollector<A, B, ResultHolder1_, Result1_> first;
    private final BiConstraintCollector<A, B, ResultHolder2_, Result2_> second;
    private final BiConstraintCollector<A, B, ResultHolder3_, Result3_> third;
    private final TriFunction<Result1_, Result2_, Result3_, Result_> composeFunction;

    private final Supplier<ResultHolder1_> firstSupplier;
    private final Supplier<ResultHolder2_> secondSupplier;
    private final Supplier<ResultHolder3_> thirdSupplier;

    private final BiConstraintCollectorAccumulator<ResultHolder1_, A, B> firstIncremental;
    private final BiConstraintCollectorAccumulator<ResultHolder2_, A, B> secondIncremental;
    private final BiConstraintCollectorAccumulator<ResultHolder3_, A, B> thirdIncremental;

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

        this.firstIncremental =
                first.isIncremental() ? first.incrementalAccumulator() : BiCollectorUtils.toIncremental(first.accumulator());
        this.secondIncremental =
                second.isIncremental() ? second.incrementalAccumulator() : BiCollectorUtils.toIncremental(second.accumulator());
        this.thirdIncremental =
                third.isIncremental() ? third.incrementalAccumulator() : BiCollectorUtils.toIncremental(third.accumulator());

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
    public @NonNull TriFunction<Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_>, A, B, Runnable> accumulator() {
        return BiCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull BiConstraintCollectorAccumulator<Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_>, A, B>
            incrementalAccumulator() {
        return ValueHandle::new;
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
        var that = (ComposeThreeBiCollector<?, ?, ?, ?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(first, that.first) && Objects.equals(second,
                that.second) && Objects.equals(third, that.third)
                && Objects.equals(composeFunction, that.composeFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, third, composeFunction);
    }

    private final class ValueHandle implements BiConstraintCollectorValueHandle<A, B> {
        private final BiConstraintCollectorValueHandle<A, B> v1;
        private final BiConstraintCollectorValueHandle<A, B> v2;
        private final BiConstraintCollectorValueHandle<A, B> v3;

        ValueHandle(Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_> container) {
            this.v1 = firstIncremental.intoGroup(container.a());
            this.v2 = secondIncremental.intoGroup(container.b());
            this.v3 = thirdIncremental.intoGroup(container.c());
        }

        @Override
        public void add(A a, B b) {
            v1.add(a, b);
            v2.add(a, b);
            v3.add(a, b);
        }

        @Override
        public void update(A a, B b) {
            v1.update(a, b);
            v2.update(a, b);
            v3.update(a, b);
        }

        @Override
        public void remove() {
            v1.remove();
            v2.remove();
            v3.remove();
        }
    }
}
