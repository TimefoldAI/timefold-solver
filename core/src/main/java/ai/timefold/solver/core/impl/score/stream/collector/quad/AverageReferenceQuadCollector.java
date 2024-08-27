package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.collector.ReferenceAverageCalculator;

final class AverageReferenceQuadCollector<A, B, C, D, Mapped_, Average_>
        extends
        ObjectCalculatorQuadCollector<A, B, C, D, Mapped_, Average_, Mapped_, ReferenceAverageCalculator<Mapped_, Average_>> {
    private final Supplier<ReferenceAverageCalculator<Mapped_, Average_>> calculatorSupplier;

    AverageReferenceQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper,
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
        AverageReferenceQuadCollector<?, ?, ?, ?, ?, ?> that = (AverageReferenceQuadCollector<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(calculatorSupplier, that.calculatorSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), calculatorSupplier);
    }
}
