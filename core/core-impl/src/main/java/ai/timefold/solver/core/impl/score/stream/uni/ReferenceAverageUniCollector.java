package ai.timefold.solver.core.impl.score.stream.uni;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.ReferenceAverageCalculator;

public final class ReferenceAverageUniCollector<A, Mapped_, Average_>
        extends ObjectCalculatorUniCollector<A, Mapped_, Average_, ReferenceAverageCalculator<Mapped_, Average_>> {
    private final Supplier<ReferenceAverageCalculator<Mapped_, Average_>> calculatorSupplier;

    private ReferenceAverageUniCollector(Function<? super A, ? extends Mapped_> mapper,
            Supplier<ReferenceAverageCalculator<Mapped_, Average_>> calculatorSupplier) {
        super(mapper);
        this.calculatorSupplier = calculatorSupplier;
    }

    public static <A> ReferenceAverageUniCollector<A, BigDecimal, BigDecimal> bigDecimal(
            Function<? super A, ? extends BigDecimal> mapper) {
        return new ReferenceAverageUniCollector<>(mapper, ReferenceAverageCalculator.bigDecimal());
    }

    public static <A> ReferenceAverageUniCollector<A, BigInteger, BigDecimal> bigInteger(
            Function<? super A, ? extends BigInteger> mapper) {
        return new ReferenceAverageUniCollector<>(mapper, ReferenceAverageCalculator.bigInteger());
    }

    public static <A> ReferenceAverageUniCollector<A, Duration, Duration> duration(
            Function<? super A, ? extends Duration> mapper) {
        return new ReferenceAverageUniCollector<>(mapper, ReferenceAverageCalculator.duration());
    }

    @Override
    public Supplier<ReferenceAverageCalculator<Mapped_, Average_>> supplier() {
        return calculatorSupplier;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        if (!super.equals(object))
            return false;
        ReferenceAverageUniCollector<?, ?, ?> that = (ReferenceAverageUniCollector<?, ?, ?>) object;
        return Objects.equals(calculatorSupplier, that.calculatorSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), calculatorSupplier);
    }
}
