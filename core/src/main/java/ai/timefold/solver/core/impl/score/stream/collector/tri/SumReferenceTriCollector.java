package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.collector.ReferenceSumCalculator;

import org.jspecify.annotations.NonNull;

final class SumReferenceTriCollector<A, B, C, Result_>
        extends ObjectCalculatorTriCollector<A, B, C, Result_, Result_, Result_, ReferenceSumCalculator<Result_>> {
    private final Result_ zero;
    private final BinaryOperator<Result_> adder;
    private final BinaryOperator<Result_> subtractor;

    SumReferenceTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper, Result_ zero,
            BinaryOperator<Result_> adder,
            BinaryOperator<Result_> subtractor) {
        super(mapper);
        this.zero = zero;
        this.adder = adder;
        this.subtractor = subtractor;
    }

    @Override
    public @NonNull Supplier<ReferenceSumCalculator<Result_>> supplier() {
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
        SumReferenceTriCollector<?, ?, ?, ?> that = (SumReferenceTriCollector<?, ?, ?, ?>) object;
        return Objects.equals(zero, that.zero) && Objects.equals(adder, that.adder) && Objects.equals(
                subtractor, that.subtractor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), zero, adder, subtractor);
    }
}
