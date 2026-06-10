package ai.timefold.solver.core.impl.bavet.tri.joiner;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.api.score.stream.tri.TriJoiner;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Combs an array of {@link TriJoiner} instances into a mergedJoiner and a mergedFiltering.
 *
 * @param <A>
 * @param <B>
 */
@NullMarked
public final class TriJoinerComber<A, B, C> {

    @SafeVarargs
    public static <A, B, C> TriJoinerComber<A, B, C> comb(TriJoiner<A, B, C>... joiners) {
        var defaultJoinerList = new ArrayList<DefaultTriJoiner<A, B, C>>(joiners.length);
        var filteringList = new ArrayList<TriPredicate<A, B, C>>(joiners.length);

        var indexOfFirstFilter = -1;
        // Make sure all indexing joiners, if any, come before filtering joiners. This is necessary for performance.
        for (var i = 0; i < joiners.length; i++) {
            var joiner = joiners[i];
            if (joiner instanceof FilteringTriJoiner) {
                // From now on, only allow filtering joiners.
                indexOfFirstFilter = i;
                filteringList.add(((FilteringTriJoiner<A, B, C>) joiner).getFilter());
            } else if (joiner instanceof DefaultTriJoiner) {
                if (indexOfFirstFilter >= 0) {
                    throw new IllegalStateException("""
                            Indexing joiner (%s) must not follow a filtering joiner (%s).
                            "Maybe reorder the joiners such that filtering() joiners are later in the parameter list."""
                            .formatted(joiner, joiners[indexOfFirstFilter]));
                }
                defaultJoinerList.add((DefaultTriJoiner<A, B, C>) joiner);
            } else {
                throw new IllegalArgumentException("The joiner class (%s) is not supported."
                        .formatted(joiner.getClass()));
            }
        }
        var mergedJoiner = DefaultTriJoiner.merge(defaultJoinerList);
        var mergedFiltering = mergeFiltering(filteringList);
        return new TriJoinerComber<>(mergedJoiner, mergedFiltering);
    }

    @SuppressWarnings("unchecked")
    private static <A, B, C> @Nullable TriPredicate<A, B, C> mergeFiltering(List<TriPredicate<A, B, C>> filteringList) {
        return switch (filteringList.size()) {
            case 0 -> null;
            case 1 -> filteringList.getFirst();
            default -> {
                // Avoid predicate.and() for debugging and potential performance
                var filteringArray = filteringList.toArray(new TriPredicate[0]);
                yield (A a, B b, C c) -> {
                    for (var predicate : filteringArray) {
                        if (!predicate.test(a, b, c)) {
                            return false;
                        }
                    }
                    return true;
                };
            }
        };
    }

    private DefaultTriJoiner<A, B, C> mergedJoiner;
    private final @Nullable TriPredicate<A, B, C> mergedFiltering;

    public TriJoinerComber(DefaultTriJoiner<A, B, C> mergedJoiner, @Nullable TriPredicate<A, B, C> mergedFiltering) {
        this.mergedJoiner = mergedJoiner;
        this.mergedFiltering = mergedFiltering;
    }

    /**
     * Returns the merged indexing joiner,
     * reordered equal-first so the indexer chain always has its (merged) equal level at the top.
     * Computed on read to also cover {@link #addJoiner} appends.
     */
    public DefaultTriJoiner<A, B, C> getMergedJoiner() {
        return mergedJoiner.reorderedEqualsFirst();
    }

    public @Nullable TriPredicate<A, B, C> getMergedFiltering() {
        return mergedFiltering;
    }

    public void addJoiner(DefaultTriJoiner<A, B, C> extraJoiner) {
        mergedJoiner = mergedJoiner.and(extraJoiner);
    }

}
