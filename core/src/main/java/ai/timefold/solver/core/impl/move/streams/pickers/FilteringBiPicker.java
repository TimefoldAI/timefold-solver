package ai.timefold.solver.core.impl.move.streams.pickers;

import java.util.Objects;

import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.pickers.BiPicker;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record FilteringBiPicker<Solution_, A, B>(FilteringBiPickerPredicate<Solution_, A, B> filter)
        implements
            BiPicker<A, B> {

    @Override
    public FilteringBiPicker<Solution_, A, B> and(BiPicker<A, B> otherPicker) {
        var castJoiner = (FilteringBiPicker<Solution_, A, B>) otherPicker;
        return new FilteringBiPicker<>(filter.and(castJoiner.filter()));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FilteringBiPicker<?, ?, ?> other
                && Objects.equals(filter, other.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(filter);
    }

    @FunctionalInterface
    public interface FilteringBiPickerPredicate<Solution_, A, B>
            extends TriPredicate<SolutionView<Solution_>, A, B> {

        default FilteringBiPickerPredicate<Solution_, A, B> and(FilteringBiPickerPredicate<Solution_, A, B> other) {
            return (solutionView, a, b) -> this.test(solutionView, a, b) && other.test(solutionView, a, b);
        }

    }

}
