package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.joiner;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.function.BiEnumeratingFilter;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.function.BiEnumeratingJoiner;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Combs an array of {@link BiEnumeratingJoiner} instances into a mergedJoiner and a mergedFiltering.
 *
 * @param <A>
 * @param <B>
 */
@NullMarked
public record BiDataJoinerComber<Solution_, A, B>(DefaultBiEnumeratingJoiner<A, B> mergedJoiner,
        @Nullable BiEnumeratingFilter<Solution_, A, B> mergedFiltering) {

    public static <Solution_, A, B> BiDataJoinerComber<Solution_, A, B> comb(BiEnumeratingJoiner<A, B>[] joiners) {
        List<DefaultBiEnumeratingJoiner<A, B>> defaultJoinerList = new ArrayList<>(joiners.length);
        List<BiEnumeratingFilter<Solution_, A, B>> filteringList = new ArrayList<>(joiners.length);

        int indexOfFirstFilter = -1;
        // Make sure all indexing joiners, if any, come before filtering joiners. This is necessary for performance.
        for (int i = 0; i < joiners.length; i++) {
            BiEnumeratingJoiner<A, B> joiner = joiners[i];
            if (joiner instanceof FilteringBiEnumeratingJoiner) {
                // From now on, only allow filtering joiners.
                indexOfFirstFilter = i;
                filteringList.add(((FilteringBiEnumeratingJoiner<Solution_, A, B>) joiner).filter());
            } else if (joiner instanceof DefaultBiEnumeratingJoiner) {
                if (indexOfFirstFilter >= 0) {
                    throw new IllegalStateException("""
                            Indexing joiner (%s) must not follow a filtering joiner (%s).
                            Maybe reorder the joiners such that filtering() joiners are later in the parameter list."""
                            .formatted(joiner, joiners[indexOfFirstFilter]));
                }
                defaultJoinerList.add((DefaultBiEnumeratingJoiner<A, B>) joiner);
            } else {
                throw new IllegalArgumentException(
                        "The joiner class (%s) is not supported.".formatted(joiner.getClass().getSimpleName()));
            }
        }
        DefaultBiEnumeratingJoiner<A, B> mergedJoiner = DefaultBiEnumeratingJoiner.merge(defaultJoinerList);
        BiEnumeratingFilter<Solution_, A, B> mergedFiltering = mergeFiltering(filteringList);
        return new BiDataJoinerComber<>(mergedJoiner, mergedFiltering);
    }

    private static <Solution_, A, B> @Nullable BiEnumeratingFilter<Solution_, A, B>
            mergeFiltering(List<BiEnumeratingFilter<Solution_, A, B>> filteringList) {
        if (filteringList.isEmpty()) {
            return null;
        }
        return switch (filteringList.size()) {
            case 1 -> filteringList.get(0);
            case 2 -> filteringList.get(0).and(filteringList.get(1));
            default ->
                // Avoid predicate.and() when more than 2 predicates for debugging and potentially performance
                (SolutionView<Solution_> solutionView, A a, B b) -> {
                    for (BiEnumeratingFilter<Solution_, A, B> predicate : filteringList) {
                        if (!predicate.test(solutionView, a, b)) {
                            return false;
                        }
                    }
                    return true;
                };
        };
    }

}
