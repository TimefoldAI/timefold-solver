package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.ReferenceAverageCalculator;

final class AverageReferenceUniCollector<A, Mapped_, Average_>
        extends ObjectCalculatorUniCollector<A, Mapped_, Average_, Mapped_, ReferenceAverageCalculator<Mapped_, Average_>> {
    private final Supplier<ReferenceAverageCalculator<Mapped_, Average_>> calculatorSupplier;

    AverageReferenceUniCollector(Function<? super A, ? extends Mapped_> mapper,
            Supplier<ReferenceAverageCalculator<Mapped_, Average_>> calculatorSupplier) {
        super(mapper);
        this.calculatorSupplier = calculatorSupplier;
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
        AverageReferenceUniCollector<?, ?, ?> that = (AverageReferenceUniCollector<?, ?, ?>) object;
        return Objects.equals(calculatorSupplier, that.calculatorSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), calculatorSupplier);
    }
}
