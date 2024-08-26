package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.ReferenceSumCalculator;

final class SumReferenceBiCollector<A, B, Result_>
        extends ObjectCalculatorBiCollector<A, B, Result_, Result_, Result_, ReferenceSumCalculator<Result_>> {
    private final Result_ zero;
    private final BinaryOperator<Result_> adder;
    private final BinaryOperator<Result_> subtractor;

    SumReferenceBiCollector(BiFunction<? super A, ? super B, ? extends Result_> mapper, Result_ zero,
            BinaryOperator<Result_> adder,
            BinaryOperator<Result_> subtractor) {
        super(mapper);
        this.zero = zero;
        this.adder = adder;
        this.subtractor = subtractor;
    }

    @Override
    public Supplier<ReferenceSumCalculator<Result_>> supplier() {
        return () -> new ReferenceSumCalculator<>(zero, adder, subtractor);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        if (!super.equals(object))
            return false;
        SumReferenceBiCollector<?, ?, ?> that = (SumReferenceBiCollector<?, ?, ?>) object;
        return Objects.equals(zero, that.zero) && Objects.equals(adder, that.adder) && Objects.equals(
                subtractor, that.subtractor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), zero, adder, subtractor);
    }
}
