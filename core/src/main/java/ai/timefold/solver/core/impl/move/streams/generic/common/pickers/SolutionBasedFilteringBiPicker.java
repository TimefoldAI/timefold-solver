package ai.timefold.solver.core.impl.move.streams.generic.common.pickers;

import java.util.Objects;
import java.util.function.BiPredicate;

import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.pickers.BiPicker;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record SolutionBasedFilteringBiPicker<Solution_, A, B>(BiPredicate<A, B> tupleFilter,
        TriPredicate<Solution_, A, B> valueFilter)
        implements
            BiPicker<A, B> {

    public static <Solution_, A, B> SolutionBasedFilteringBiPicker<Solution_, A, B>
            wrap(FilteringBiPicker<A, B> filteringBiPicker, TriPredicate<Solution_, A, B> valueFilter) {
        return new SolutionBasedFilteringBiPicker<>(filteringBiPicker.filter(), valueFilter);
    }

    @Override
    public SolutionBasedFilteringBiPicker<Solution_, A, B> and(BiPicker<A, B> otherPicker) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SolutionBasedFilteringBiPicker<?, ?, ?> other
                && Objects.equals(tupleFilter, other.tupleFilter)
                && Objects.equals(valueFilter, other.valueFilter);
    }

    @Override
    public int hashCode() {
        var hashCode = 31;
        hashCode = hashCode * 31 + Objects.hashCode(tupleFilter);
        hashCode = hashCode * 31 + Objects.hashCode(valueFilter);
        return hashCode;
    }

}
