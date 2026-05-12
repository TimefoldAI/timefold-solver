package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractReferenceSumSlot;

import org.jspecify.annotations.NonNull;

final class SumReferenceQuadCollector<A, B, C, D, Result_>
        extends
        ObjectCalculatorQuadCollector<A, B, C, D, Result_, Result_, AbstractReferenceSumSlot.State<Result_>> {
    private final Result_ zero;
    private final BinaryOperator<Result_> adder;
    private final BinaryOperator<Result_> subtractor;

    SumReferenceQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result_> mapper,
            Result_ zero,
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
    protected QuadConstraintCollectorValueHandle<A, B, C, D> newAccumulatedValue(
            AbstractReferenceSumSlot.State<Result_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractReferenceSumSlot<Result_>
            implements QuadConstraintCollectorValueHandle<A, B, C, D> {
        Slot(AbstractReferenceSumSlot.State<Result_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b, C c, D d) {
            addMapped(mapper.apply(a, b, c, d));
        }

        @Override
        public void update(A a, B b, C c, D d) {
            updateMapped(mapper.apply(a, b, c, d));
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
        SumReferenceQuadCollector<?, ?, ?, ?, ?> that = (SumReferenceQuadCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(zero, that.zero) && Objects.equals(adder, that.adder) && Objects.equals(
                subtractor, that.subtractor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), zero, adder, subtractor);
    }
}
