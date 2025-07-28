package ai.timefold.solver.core.impl.move.streams.dataset.joiner;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataFilter;
import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataJoiner;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Combs an array of {@link BiDataJoiner} instances into a mergedJoiner and a mergedFiltering.
 *
 * @param <A>
 * @param <B>
 */
@NullMarked
public record BiDataJoinerComber<Solution_, A, B>(DefaultBiDataJoiner<A, B> mergedJoiner, @Nullable BiDataFilter<Solution_, A, B> mergedFiltering) {

    public static <Solution_, A, B> BiDataJoinerComber<Solution_, A, B> comb(BiDataJoiner<A, B>[] joiners) {
        List<DefaultBiDataJoiner<A, B>> defaultJoinerList = new ArrayList<>(joiners.length);
        List<BiDataFilter<Solution_, A, B>> filteringList = new ArrayList<>(joiners.length);

        int indexOfFirstFilter = -1;
        // Make sure all indexing joiners, if any, come before filtering joiners. This is necessary for performance.
        for (int i = 0; i < joiners.length; i++) {
            BiDataJoiner<A, B> joiner = joiners[i];
            if (joiner instanceof FilteringBiDataJoiner) {
                // From now on, only allow filtering joiners.
                indexOfFirstFilter = i;
                filteringList.add(((FilteringBiDataJoiner<Solution_, A, B>) joiner).filter());
            } else if (joiner instanceof DefaultBiDataJoiner) {
                if (indexOfFirstFilter >= 0) {
                    throw new IllegalStateException("""
                            Indexing joiner (%s) must not follow a filtering joiner (%s).
                            Maybe reorder the joiners such that filtering() joiners are later in the parameter list."""
                            .formatted(joiner, joiners[indexOfFirstFilter]));
                }
                defaultJoinerList.add((DefaultBiDataJoiner<A, B>) joiner);
            } else {
                throw new IllegalArgumentException(
                        "The joiner class (%s) is not supported.".formatted(joiner.getClass().getSimpleName()));
            }
        }
        DefaultBiDataJoiner<A, B> mergedJoiner = DefaultBiDataJoiner.merge(defaultJoinerList);
        BiDataFilter<Solution_, A, B> mergedFiltering = mergeFiltering(filteringList);
        return new BiDataJoinerComber<>(mergedJoiner, mergedFiltering);
    }

    private static <Solution_, A, B> @Nullable BiDataFilter<Solution_, A, B>
            mergeFiltering(List<BiDataFilter<Solution_, A, B>> filteringList) {
        if (filteringList.isEmpty()) {
            return null;
        }
        return switch (filteringList.size()) {
            case 1 -> filteringList.get(0);
            case 2 -> filteringList.get(0).and(filteringList.get(1));
            default ->
                // Avoid predicate.and() when more than 2 predicates for debugging and potentially performance
                (SolutionView<Solution_> solutionView, A a, B b) -> {
                    for (BiDataFilter<Solution_, A, B> predicate : filteringList) {
                        if (!predicate.test(solutionView, a, b)) {
                            return false;
                        }
                    }
                    return true;
                };
        };
    }

}
