package ai.timefold.solver.core.impl.score.stream.tri;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.ReferenceSumCalculator;

public final class ReferenceSumTriCollector<A, B, C, Result>
        extends ObjectCalculatorTriCollector<A, B, C, Result, Result, ReferenceSumCalculator<Result>> {
    private final Result zero;
    private final BinaryOperator<Result> adder;
    private final BinaryOperator<Result> subtractor;

    public ReferenceSumTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Result> mapper, Result zero,
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
        ReferenceSumTriCollector<?, ?, ?, ?> that = (ReferenceSumTriCollector<?, ?, ?, ?>) object;
        return Objects.equals(zero, that.zero) && Objects.equals(adder, that.adder) && Objects.equals(
                subtractor, that.subtractor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), zero, adder, subtractor);
    }
}
