package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.ReferenceAverageCalculator;

final class AverageReferenceBiCollector<A, B, Mapped_, Average_>
        extends ObjectCalculatorBiCollector<A, B, Mapped_, Average_, Mapped_, ReferenceAverageCalculator<Mapped_, Average_>> {
    private final Supplier<ReferenceAverageCalculator<Mapped_, Average_>> calculatorSupplier;

    AverageReferenceBiCollector(BiFunction<? super A, ? super B, ? extends Mapped_> mapper,
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
        AverageReferenceBiCollector<?, ?, ?, ?> that = (AverageReferenceBiCollector<?, ?, ?, ?>) object;
        return Objects.equals(calculatorSupplier, that.calculatorSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), calculatorSupplier);
    }
}
