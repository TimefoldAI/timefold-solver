package ai.timefold.solver.core.impl.bavet.bi.joiner;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Combs an array of {@link BiJoiner} instances into a mergedJoiner and a mergedFiltering.
 *
 * @param <A>
 * @param <B>
 */
@NullMarked
public final class BiJoinerComber<A, B> {

    @SafeVarargs
    public static <A, B> BiJoinerComber<A, B> comb(BiJoiner<A, B>... joiners) {
        var defaultJoinerList = new ArrayList<DefaultBiJoiner<A, B>>(joiners.length);
        var filteringList = new ArrayList<BiPredicate<A, B>>(joiners.length);

        var indexOfFirstFilter = -1;
        // Make sure all indexing joiners, if any, come before filtering joiners. This is necessary for performance.
        for (var i = 0; i < joiners.length; i++) {
            var joiner = joiners[i];
            if (joiner instanceof FilteringBiJoiner) {
                // From now on, only allow filtering joiners.
                indexOfFirstFilter = i;
                filteringList.add(((FilteringBiJoiner<A, B>) joiner).getFilter());
            } else if (joiner instanceof DefaultBiJoiner) {
                if (indexOfFirstFilter >= 0) {
                    throw new IllegalStateException("""
                            Indexing joiner (%s) must not follow a filtering joiner (%s).
                            "Maybe reorder the joiners such that filtering() joiners are later in the parameter list."""
                            .formatted(joiner, joiners[indexOfFirstFilter]));
                }
                defaultJoinerList.add((DefaultBiJoiner<A, B>) joiner);
            } else {
                throw new IllegalArgumentException("The joiner class (%s) is not supported."
                        .formatted(joiner.getClass()));
            }
        }
        var mergedJoiner = DefaultBiJoiner.merge(defaultJoinerList);
        var mergedFiltering = mergeFiltering(filteringList);
        return new BiJoinerComber<>(mergedJoiner, mergedFiltering);
    }

    @SuppressWarnings("unchecked")
    private static <A, B> @Nullable BiPredicate<A, B> mergeFiltering(List<BiPredicate<A, B>> filteringList) {
        return switch (filteringList.size()) {
            case 0 -> null;
            case 1 -> filteringList.getFirst();
            default -> {
                // Avoid predicate.and() for debugging and potential performance
                var filteringArray = filteringList.toArray(new BiPredicate[0]);
                yield (A a, B b) -> {
                    for (var predicate : filteringArray) {
                        if (!predicate.test(a, b)) {
                            return false;
                        }
                    }
                    return true;
                };
            }
        };
    }

    private DefaultBiJoiner<A, B> mergedJoiner;
    private final @Nullable BiPredicate<A, B> mergedFiltering;

    public BiJoinerComber(DefaultBiJoiner<A, B> mergedJoiner, @Nullable BiPredicate<A, B> mergedFiltering) {
        this.mergedJoiner = mergedJoiner;
        this.mergedFiltering = mergedFiltering;
    }

    /**
     * Returns the merged indexing joiner,
     * reordered equal-first so the indexer chain always has its (merged) equal level at the top.
     * Computed on read to also cover {@link #addJoiner} appends.
     */
    public DefaultBiJoiner<A, B> getMergedJoiner() {
        return mergedJoiner.reorderedEqualsFirst();
    }

    public @Nullable BiPredicate<A, B> getMergedFiltering() {
        return mergedFiltering;
    }

    public void addJoiner(DefaultBiJoiner<A, B> extraJoiner) {
        mergedJoiner = mergedJoiner.and(extraJoiner);
    }

}
