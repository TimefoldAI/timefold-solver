package ai.timefold.solver.core.impl.score.stream.tri;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.ReferenceAverageCalculator;

public final class ReferenceAverageTriCollector<A, B, C, Mapped_, Average_>
        extends ObjectCalculatorTriCollector<A, B, C, Mapped_, Average_, ReferenceAverageCalculator<Mapped_, Average_>> {
    private final Supplier<ReferenceAverageCalculator<Mapped_, Average_>> calculatorSupplier;

    private ReferenceAverageTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper,
            Supplier<ReferenceAverageCalculator<Mapped_, Average_>> calculatorSupplier) {
        super(mapper);
        this.calculatorSupplier = calculatorSupplier;
    }

    public static <A, B, C> ReferenceAverageTriCollector<A, B, C, BigDecimal, BigDecimal> bigDecimal(
            TriFunction<? super A, ? super B, ? super C, ? extends BigDecimal> mapper) {
        return new ReferenceAverageTriCollector<>(mapper, ReferenceAverageCalculator.bigDecimal());
    }

    public static <A, B, C> ReferenceAverageTriCollector<A, B, C, BigInteger, BigDecimal> bigInteger(
            TriFunction<? super A, ? super B, ? super C, ? extends BigInteger> mapper) {
        return new ReferenceAverageTriCollector<>(mapper, ReferenceAverageCalculator.bigInteger());
    }

    public static <A, B, C> ReferenceAverageTriCollector<A, B, C, Duration, Duration> duration(
            TriFunction<? super A, ? super B, ? super C, ? extends Duration> mapper) {
        return new ReferenceAverageTriCollector<>(mapper, ReferenceAverageCalculator.duration());
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
        ReferenceAverageTriCollector<?, ?, ?, ?, ?> that = (ReferenceAverageTriCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(calculatorSupplier, that.calculatorSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), calculatorSupplier);
    }
}
