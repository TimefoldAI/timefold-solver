package ai.timefold.solver.core.impl.score.stream.quad;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.ReferenceAverageCalculator;

public final class ReferenceAverageQuadCollector<A, B, C, D, Mapped_, Average_>
        extends ObjectCalculatorQuadCollector<A, B, C, D, Mapped_, Average_, ReferenceAverageCalculator<Mapped_, Average_>> {
    private final Supplier<ReferenceAverageCalculator<Mapped_, Average_>> calculatorSupplier;

    private ReferenceAverageQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper,
            Supplier<ReferenceAverageCalculator<Mapped_, Average_>> calculatorSupplier) {
        super(mapper);
        this.calculatorSupplier = calculatorSupplier;
    }

    public static <A, B, C, D> ReferenceAverageQuadCollector<A, B, C, D, BigDecimal, BigDecimal> bigDecimal(
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends BigDecimal> mapper) {
        return new ReferenceAverageQuadCollector<>(mapper, ReferenceAverageCalculator.bigDecimal());
    }

    public static <A, B, C, D> ReferenceAverageQuadCollector<A, B, C, D, BigInteger, BigDecimal> bigInteger(
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends BigInteger> mapper) {
        return new ReferenceAverageQuadCollector<>(mapper, ReferenceAverageCalculator.bigInteger());
    }

    public static <A, B, C, D> ReferenceAverageQuadCollector<A, B, C, D, Duration, Duration> duration(
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Duration> mapper) {
        return new ReferenceAverageQuadCollector<>(mapper, ReferenceAverageCalculator.duration());
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
        ReferenceAverageQuadCollector<?, ?, ?, ?, ?, ?> that = (ReferenceAverageQuadCollector<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(calculatorSupplier, that.calculatorSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), calculatorSupplier);
    }
}
