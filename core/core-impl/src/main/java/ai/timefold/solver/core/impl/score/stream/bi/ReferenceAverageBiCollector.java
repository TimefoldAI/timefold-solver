package ai.timefold.solver.core.impl.score.stream.bi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.ReferenceAverageCalculator;

public final class ReferenceAverageBiCollector<A, B, Mapped_, Average_>
        extends ObjectCalculatorBiCollector<A, B, Mapped_, Average_, ReferenceAverageCalculator<Mapped_, Average_>> {
    private final Supplier<ReferenceAverageCalculator<Mapped_, Average_>> calculatorSupplier;

    private ReferenceAverageBiCollector(BiFunction<? super A, ? super B, ? extends Mapped_> mapper,
            Supplier<ReferenceAverageCalculator<Mapped_, Average_>> calculatorSupplier) {
        super(mapper);
        this.calculatorSupplier = calculatorSupplier;
    }

    public static <A, B> ReferenceAverageBiCollector<A, B, BigDecimal, BigDecimal> bigDecimal(
            BiFunction<? super A, ? super B, ? extends BigDecimal> mapper) {
        return new ReferenceAverageBiCollector<>(mapper, ReferenceAverageCalculator.bigDecimal());
    }

    public static <A, B> ReferenceAverageBiCollector<A, B, BigInteger, BigDecimal> bigInteger(
            BiFunction<? super A, ? super B, ? extends BigInteger> mapper) {
        return new ReferenceAverageBiCollector<>(mapper, ReferenceAverageCalculator.bigInteger());
    }

    public static <A, B> ReferenceAverageBiCollector<A, B, Duration, Duration> duration(
            BiFunction<? super A, ? super B, ? extends Duration> mapper) {
        return new ReferenceAverageBiCollector<>(mapper, ReferenceAverageCalculator.duration());
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
        ReferenceAverageBiCollector<?, ?, ?, ?> that = (ReferenceAverageBiCollector<?, ?, ?, ?>) object;
        return Objects.equals(calculatorSupplier, that.calculatorSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), calculatorSupplier);
    }
}
