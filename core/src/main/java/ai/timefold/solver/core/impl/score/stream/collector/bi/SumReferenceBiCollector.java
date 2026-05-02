package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractReferenceSumSlot;

import org.jspecify.annotations.NonNull;

final class SumReferenceBiCollector<A, B, Result_>
        extends
        ObjectCalculatorBiCollector<A, B, Result_, Result_, AbstractReferenceSumSlot.State<Result_>> {
    private final Result_ zero;
    private final BinaryOperator<Result_> adder;
    private final BinaryOperator<Result_> subtractor;

    SumReferenceBiCollector(BiFunction<? super A, ? super B, ? extends Result_> mapper, Result_ zero,
            BinaryOperator<Result_> adder,
            BinaryOperator<Result_> subtractor) {
        super(mapper);
        this.zero = zero;
        this.adder = adder;
        this.subtractor = subtractor;
    }

    @Override
    public @NonNull Supplier<AbstractReferenceSumSlot.State<Result_>> supplier() {
        return () -> new AbstractReferenceSumSlot.State<>(zero, adder, subtractor);
    }

    @Override
    public @NonNull Function<AbstractReferenceSumSlot.State<Result_>, Result_> finisher() {
        return AbstractReferenceSumSlot.State::result;
    }

    @Override
    protected BiConstraintCollectorAccumulatedValue<A, B> newAccumulatedValue(AbstractReferenceSumSlot.State<Result_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractReferenceSumSlot<Result_>
            implements BiConstraintCollectorAccumulatedValue<A, B> {
        Slot(AbstractReferenceSumSlot.State<Result_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b) {
            addMapped(mapper.apply(a, b));
        }

        @Override
        public void update(A a, B b) {
            updateMapped(mapper.apply(a, b));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        if (!super.equals(object))
            return false;
        SumReferenceBiCollector<?, ?, ?> that = (SumReferenceBiCollector<?, ?, ?>) object;
        return Objects.equals(zero, that.zero) && Objects.equals(adder, that.adder) && Objects.equals(
                subtractor, that.subtractor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), zero, adder, subtractor);
    }
}
