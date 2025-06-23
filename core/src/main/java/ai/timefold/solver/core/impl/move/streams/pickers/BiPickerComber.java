package ai.timefold.solver.core.impl.move.streams.pickers;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.pickers.BiPicker;
import ai.timefold.solver.core.impl.move.streams.pickers.FilteringBiPicker.FilteringBiPickerPredicate;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Combs an array of {@link BiPicker} instances into a {@link #mergedPicker()} and {@link #mergedFiltering()}.
 *
 * @param <A>
 * @param <B>
 * @param mergedPicker the merged {@link DefaultBiPicker} from all indexing pickers
 * @param mergedFiltering null if not applicable
 */
@NullMarked
public record BiPickerComber<Solution_, A, B>(DefaultBiPicker<A, B> mergedPicker,
        @Nullable FilteringBiPickerPredicate<Solution_, A, B> mergedFiltering) {

    @SuppressWarnings("unchecked")
    public static <Solution_, A, B> BiPickerComber<Solution_, A, B> comb(BiPicker<A, B>[] pickers) {
        var defaultPickerList = new ArrayList<DefaultBiPicker<A, B>>(pickers.length);
        var filteringList = new ArrayList<FilteringBiPickerPredicate<Solution_, A, B>>(pickers.length);

        var indexOfFirstFilter = -1;
        // Make sure all indexing pickers, if any, come before filtering pickers. This is necessary for performance.
        for (var i = 0; i < pickers.length; i++) {
            var picker = pickers[i];
            if (picker instanceof FilteringBiPicker<?, A, B> filteringBiPicker) {
                // From now on, only allow filtering joiners.
                indexOfFirstFilter = i;
                filteringList.add((FilteringBiPickerPredicate<Solution_, A, B>) filteringBiPicker.filter());
            } else if (picker instanceof DefaultBiPicker<A, B> defaultBiPicker) {
                if (indexOfFirstFilter >= 0) {
                    throw new IllegalStateException("""
                            Indexing picker (%s) must not follow a filtering picker (%s).
                            Maybe reorder the pickers such that filtering() pickers are later in the parameter list."""
                            .formatted(picker, pickers[indexOfFirstFilter]));
                }
                defaultPickerList.add(defaultBiPicker);
            } else {
                throw new IllegalArgumentException("The picker class (%s) is not supported."
                        .formatted(picker.getClass().getCanonicalName()));
            }
        }
        var mergedPicker = DefaultBiPicker.merge(defaultPickerList);
        var mergedFiltering = mergeFiltering(filteringList);
        return new BiPickerComber<>(mergedPicker, mergedFiltering);
    }

    private static <Solution_, A, B> @Nullable FilteringBiPickerPredicate<Solution_, A, B>
            mergeFiltering(List<FilteringBiPickerPredicate<Solution_, A, B>> filteringList) {
        if (filteringList.isEmpty()) {
            return null;
        } else if (filteringList.size() == 1) {
            return filteringList.get(0);
        }
        // Avoid predicate.and() for debugging and potentially performance.
        return (SolutionView<Solution_> solutionView, A a, B b) -> {
            for (var predicate : filteringList) {
                if (!predicate.test(solutionView, a, b)) {
                    return false;
                }
            }
            return true;
        };
    }

}
