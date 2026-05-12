package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractReferenceSumSlot;

import org.jspecify.annotations.NonNull;

final class SumReferenceUniCollector<A, Result_>
        extends ObjectCalculatorUniCollector<A, Result_, Result_, AbstractReferenceSumSlot.State<Result_>> {
    private final Result_ zero;
    private final BinaryOperator<Result_> adder;
    private final BinaryOperator<Result_> subtractor;

    SumReferenceUniCollector(Function<? super A, ? extends Result_> mapper, Result_ zero, BinaryOperator<Result_> adder,
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
    protected UniConstraintCollectorValueHandle<A>
            newAccumulatedValue(AbstractReferenceSumSlot.State<Result_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractReferenceSumSlot<Result_>
            implements UniConstraintCollectorValueHandle<A> {
        Slot(AbstractReferenceSumSlot.State<Result_> state) {
            super(state);
        }

        @Override
        public void add(A a) {
            addMapped(mapper.apply(a));
        }

        @Override
        public void replaceWith(A a) {
            replaceWithMapped(mapper.apply(a));
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
        SumReferenceUniCollector<?, ?> that = (SumReferenceUniCollector<?, ?>) object;
        return Objects.equals(zero, that.zero) && Objects.equals(adder, that.adder) && Objects.equals(
                subtractor, that.subtractor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), zero, adder, subtractor);
    }
}
