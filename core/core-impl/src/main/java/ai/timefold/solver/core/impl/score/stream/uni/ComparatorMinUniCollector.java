package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.MinMaxUndoableActionable;

public final class ComparatorMinUniCollector<A, Result_>
        extends UndoableActionableUniCollector<A, Result_, Result_, MinMaxUndoableActionable<Result_, Result_>> {
    private final Comparator<? super Result_> comparator;

    public ComparatorMinUniCollector(Function<? super A, ? extends Result_> mapper, Comparator<? super Result_> comparator) {
        super(mapper);
        this.comparator = comparator;
    }

    @Override
    public Supplier<MinMaxUndoableActionable<Result_, Result_>> supplier() {
        return () -> MinMaxUndoableActionable.minCalculator(comparator);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        if (!super.equals(object))
            return false;
        ComparatorMinUniCollector<?, ?> that = (ComparatorMinUniCollector<?, ?>) object;
        return Objects.equals(comparator, that.comparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), comparator);
    }
}