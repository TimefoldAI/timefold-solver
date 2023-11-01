package ai.timefold.solver.core.impl.score.stream.quad;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.ReferenceSumCalculator;

public final class ReferenceSumQuadCollector<A, B, C, D, Result>
        extends ObjectCalculatorQuadCollector<A, B, C, D, Result, Result, ReferenceSumCalculator<Result>> {
    private final Result zero;
    private final BinaryOperator<Result> adder;
    private final BinaryOperator<Result> subtractor;

    public ReferenceSumQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result> mapper,
            Result zero,
            BinaryOperator<Result> adder,
            BinaryOperator<Result> subtractor) {
        super(mapper);
        this.zero = zero;
        this.adder = adder;
        this.subtractor = subtractor;
    }

    @Override
    public Supplier<ReferenceSumCalculator<Result>> supplier() {
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
        ReferenceSumQuadCollector<?, ?, ?, ?, ?> that = (ReferenceSumQuadCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(zero, that.zero) && Objects.equals(adder, that.adder) && Objects.equals(
                subtractor, that.subtractor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), zero, adder, subtractor);
    }
}
