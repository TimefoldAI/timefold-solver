package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.ReferenceSumCalculator;

public class ReferenceSumUniCollector<A, Result>
        extends ObjectCalculatorUniCollector<A, Result, Result, ReferenceSumCalculator<Result>> {
    private final Result zero;
    private final BinaryOperator<Result> adder;
    private final BinaryOperator<Result> subtractor;

    public ReferenceSumUniCollector(Function<? super A, ? extends Result> mapper, Result zero, BinaryOperator<Result> adder,
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
        ReferenceSumUniCollector<?, ?> that = (ReferenceSumUniCollector<?, ?>) object;
        return Objects.equals(zero, that.zero) && Objects.equals(adder, that.adder) && Objects.equals(
                subtractor, that.subtractor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), zero, adder, subtractor);
    }
}
