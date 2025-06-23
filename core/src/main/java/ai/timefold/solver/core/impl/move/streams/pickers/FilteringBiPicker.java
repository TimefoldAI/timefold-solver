package ai.timefold.solver.core.impl.move.streams.pickers;

import java.util.Objects;
import java.util.function.BiPredicate;

import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.pickers.BiPicker;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record FilteringBiPicker<Solution_, A, B>(FilteringBiPickerPredicate<Solution_, A, B> filter)
        implements
            BiPicker<A, B> {

    public static <Solution_, A, B> FilteringBiPicker<Solution_, A, B> of(BiPredicate<A, B> filter) {
        return new FilteringBiPicker<>(new WrappedPredicate<>(filter));
    }

    @Override
    public FilteringBiPicker<Solution_, A, B> and(BiPicker<A, B> otherPicker) {
        var castJoiner = (FilteringBiPicker<Solution_, A, B>) otherPicker;
        return new FilteringBiPicker<>(filter.and(castJoiner.filter()));
    }

    @FunctionalInterface
    public interface FilteringBiPickerPredicate<Solution_, A, B>
            extends TriPredicate<SolutionView<Solution_>, A, B> {

        default FilteringBiPickerPredicate<Solution_, A, B> and(FilteringBiPickerPredicate<Solution_, A, B> other) {
            return (solutionView, a, b) -> this.test(solutionView, a, b) && other.test(solutionView, a, b);
        }

    }

    /**
     * Exists to make sure that node sharing still works.
     * Instances of this class need to equal if the underlying predicate is equal.
     */
    private record WrappedPredicate<Solution_, A, B>(BiPredicate<A, B> predicate)
            implements
                FilteringBiPickerPredicate<Solution_, A, B> {

        private WrappedPredicate(BiPredicate<A, B> predicate) {
            this.predicate = Objects.requireNonNull(predicate);
        }

        @Override
        public boolean test(SolutionView<Solution_> solutionView, A a, B b) {
            return predicate.test(a, b);
        }

    }

}
