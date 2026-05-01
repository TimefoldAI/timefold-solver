package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.ReferenceAverageCalculator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class AverageReferenceUniCollector<A, Mapped_, Average_>
        extends
        ObjectCalculatorUniCollector<A, Mapped_, Average_, ReferenceAverageCalculator.State<Mapped_, Average_>, ReferenceAverageCalculator<Mapped_, Average_>> {
    private final Supplier<ReferenceAverageCalculator.State<Mapped_, Average_>> stateSupplier;

    AverageReferenceUniCollector(Function<? super A, ? extends Mapped_> mapper,
            Supplier<ReferenceAverageCalculator.State<Mapped_, Average_>> stateSupplier) {
        super(mapper);
        this.stateSupplier = stateSupplier;
    }

    @Override
    protected ReferenceAverageCalculator<Mapped_, Average_>
            newCalculator(ReferenceAverageCalculator.State<Mapped_, Average_> state) {
        return new ReferenceAverageCalculator<>(state);
    }

    @Override
    public @NonNull Supplier<ReferenceAverageCalculator.State<Mapped_, Average_>> supplier() {
        return stateSupplier;
    }

    @Override
    public @NonNull Function<ReferenceAverageCalculator.State<Mapped_, Average_>, @Nullable Average_> finisher() {
        return ReferenceAverageCalculator.State::result;
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
        return Objects.equals(stateSupplier, that.stateSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), stateSupplier);
    }
}
