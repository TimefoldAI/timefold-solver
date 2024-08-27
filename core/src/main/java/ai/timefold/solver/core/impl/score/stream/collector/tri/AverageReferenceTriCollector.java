package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.collector.ReferenceAverageCalculator;

final class AverageReferenceTriCollector<A, B, C, Mapped_, Average_>
        extends
        ObjectCalculatorTriCollector<A, B, C, Mapped_, Average_, Mapped_, ReferenceAverageCalculator<Mapped_, Average_>> {
    private final Supplier<ReferenceAverageCalculator<Mapped_, Average_>> calculatorSupplier;

    AverageReferenceTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper,
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
        AverageReferenceTriCollector<?, ?, ?, ?, ?> that = (AverageReferenceTriCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(calculatorSupplier, that.calculatorSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), calculatorSupplier);
    }
}
