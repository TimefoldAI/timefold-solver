package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.util.Triple;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class ComposeThreeQuadCollector<A, B, C, D, ResultHolder1_, ResultHolder2_, ResultHolder3_, Result1_, Result2_, Result3_, Result_>
        implements QuadConstraintCollector<A, B, C, D, Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_>, Result_> {
    private final QuadConstraintCollector<A, B, C, D, ResultHolder1_, Result1_> first;
    private final QuadConstraintCollector<A, B, C, D, ResultHolder2_, Result2_> second;
    private final QuadConstraintCollector<A, B, C, D, ResultHolder3_, Result3_> third;
    private final TriFunction<Result1_, Result2_, Result3_, Result_> composeFunction;

    private final Supplier<ResultHolder1_> firstSupplier;
    private final Supplier<ResultHolder2_> secondSupplier;
    private final Supplier<ResultHolder3_> thirdSupplier;

    private final QuadConstraintCollectorAccumulator<ResultHolder1_, A, B, C, D> firstIncremental;
    private final QuadConstraintCollectorAccumulator<ResultHolder2_, A, B, C, D> secondIncremental;
    private final QuadConstraintCollectorAccumulator<ResultHolder3_, A, B, C, D> thirdIncremental;

    private final Function<ResultHolder1_, Result1_> firstFinisher;
    private final Function<ResultHolder2_, Result2_> secondFinisher;
    private final Function<ResultHolder3_, Result3_> thirdFinisher;

    ComposeThreeQuadCollector(QuadConstraintCollector<A, B, C, D, ResultHolder1_, Result1_> first,
            QuadConstraintCollector<A, B, C, D, ResultHolder2_, Result2_> second,
            QuadConstraintCollector<A, B, C, D, ResultHolder3_, Result3_> third,
            TriFunction<Result1_, Result2_, Result3_, Result_> composeFunction) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.composeFunction = composeFunction;

        this.firstSupplier = first.supplier();
        this.secondSupplier = second.supplier();
        this.thirdSupplier = third.supplier();

        this.firstIncremental = first.isIncremental() ? first.incrementalAccumulator()
                : QuadCollectorUtils.toIncremental(first.accumulator());
        this.secondIncremental = second.isIncremental() ? second.incrementalAccumulator()
                : QuadCollectorUtils.toIncremental(second.accumulator());
        this.thirdIncremental = third.isIncremental() ? third.incrementalAccumulator()
                : QuadCollectorUtils.toIncremental(third.accumulator());

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
    public @NonNull PentaFunction<Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_>, A, B, C, D, Runnable> accumulator() {
        return QuadCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull QuadConstraintCollectorAccumulator<Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_>, A, B, C, D>
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
        var that = (ComposeThreeQuadCollector<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(first, that.first) && Objects.equals(second,
                that.second) && Objects.equals(third, that.third)
                && Objects.equals(composeFunction,
                        that.composeFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, third, composeFunction);
    }

    private final class ValueHandle implements QuadConstraintCollectorValueHandle<A, B, C, D> {
        private final QuadConstraintCollectorValueHandle<A, B, C, D> v1;
        private final QuadConstraintCollectorValueHandle<A, B, C, D> v2;
        private final QuadConstraintCollectorValueHandle<A, B, C, D> v3;

        ValueHandle(Triple<ResultHolder1_, ResultHolder2_, ResultHolder3_> container) {
            this.v1 = firstIncremental.intoGroup(container.a());
            this.v2 = secondIncremental.intoGroup(container.b());
            this.v3 = thirdIncremental.intoGroup(container.c());
        }

        @Override
        public void add(A a, B b, C c, D d) {
            v1.add(a, b, c, d);
            v2.add(a, b, c, d);
            v3.add(a, b, c, d);
        }

        @Override
        public void replaceWith(A a, B b, C c, D d) {
            v1.replaceWith(a, b, c, d);
            v2.replaceWith(a, b, c, d);
            v3.replaceWith(a, b, c, d);
        }

        @Override
        public void remove() {
            v1.remove();
            v2.remove();
            v3.remove();
        }
    }
}
