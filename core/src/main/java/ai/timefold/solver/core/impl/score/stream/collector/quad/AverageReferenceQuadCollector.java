package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.collector.ReferenceAverageCalculator;

import org.jspecify.annotations.NonNull;

final class AverageReferenceQuadCollector<A, B, C, D, Mapped_, Average_>
        extends
        ObjectCalculatorQuadCollector<A, B, C, D, Mapped_, Average_, ReferenceAverageCalculator.State<Mapped_, Average_>, ReferenceAverageCalculator<Mapped_, Average_>> {
    private final Supplier<ReferenceAverageCalculator.State<Mapped_, Average_>> stateSupplier;

    AverageReferenceQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper,
            Supplier<ReferenceAverageCalculator.State<Mapped_, Average_>> stateSupplier) {
        super(mapper);
        this.stateSupplier = stateSupplier;
    }

    @Override
    public @NonNull Supplier<ReferenceAverageCalculator.State<Mapped_, Average_>> supplier() {
        return stateSupplier;
    }

    @Override
    public @NonNull Function<ReferenceAverageCalculator.State<Mapped_, Average_>, Average_> finisher() {
        return ReferenceAverageCalculator.State::result;
    }

    @Override
    protected ReferenceAverageCalculator<Mapped_, Average_> newCalculator(
            ReferenceAverageCalculator.State<Mapped_, Average_> state) {
        return new ReferenceAverageCalculator<>(state);
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
        return Objects.equals(stateSupplier, that.stateSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), stateSupplier);
    }
}
