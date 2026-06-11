package ai.timefold.solver.core.impl.bavet.quad.joiner;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.score.stream.quad.QuadJoiner;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Combs an array of {@link QuadJoiner} instances into a mergedJoiner and a mergedFiltering.
 *
 * @param <A>
 * @param <B>
 */
@NullMarked
public final class QuadJoinerComber<A, B, C, D> {

    @SafeVarargs
    public static <A, B, C, D> QuadJoinerComber<A, B, C, D> comb(QuadJoiner<A, B, C, D>... joiners) {
        var defaultJoinerList = new ArrayList<DefaultQuadJoiner<A, B, C, D>>(joiners.length);
        var filteringList = new ArrayList<QuadPredicate<A, B, C, D>>(joiners.length);

        var indexOfFirstFilter = -1;
        // Make sure all indexing joiners, if any, come before filtering joiners. This is necessary for performance.
        for (var i = 0; i < joiners.length; i++) {
            var joiner = joiners[i];
            if (joiner instanceof FilteringQuadJoiner) {
                // From now on, only allow filtering joiners.
                indexOfFirstFilter = i;
                filteringList.add(((FilteringQuadJoiner<A, B, C, D>) joiner).getFilter());
            } else if (joiner instanceof DefaultQuadJoiner) {
                if (indexOfFirstFilter >= 0) {
                    throw new IllegalStateException("""
                            Indexing joiner (%s) must not follow a filtering joiner (%s).
                            Maybe reorder the joiners such that filtering() joiners are later in the parameter list."""
                            .formatted(joiner, joiners[indexOfFirstFilter]));
                }
                defaultJoinerList.add((DefaultQuadJoiner<A, B, C, D>) joiner);
            } else {
                throw new IllegalArgumentException("The joiner class (%s) is not supported."
                        .formatted(joiner.getClass()));
            }
        }
        var mergedJoiner = DefaultQuadJoiner.merge(defaultJoinerList);
        var mergedFiltering = mergeFiltering(filteringList);
        return new QuadJoinerComber<>(mergedJoiner, mergedFiltering);
    }

    @SuppressWarnings("unchecked")
    private static <A, B, C, D> @Nullable QuadPredicate<A, B, C, D>
            mergeFiltering(List<QuadPredicate<A, B, C, D>> filteringList) {
        return switch (filteringList.size()) {
            case 0 -> null;
            case 1 -> filteringList.getFirst();
            default -> {
                // Avoid predicate.and() for debugging and potential performance
                var filteringArray = filteringList.toArray(new QuadPredicate[0]);
                yield (A a, B b, C c, D d) -> {
                    for (var predicate : filteringArray) {
                        if (!predicate.test(a, b, c, d)) {
                            return false;
                        }
                    }
                    return true;
                };
            }
        };
    }

    private DefaultQuadJoiner<A, B, C, D> mergedJoiner;
    private final @Nullable QuadPredicate<A, B, C, D> mergedFiltering;

    public QuadJoinerComber(DefaultQuadJoiner<A, B, C, D> mergedJoiner, @Nullable QuadPredicate<A, B, C, D> mergedFiltering) {
        this.mergedJoiner = mergedJoiner;
        this.mergedFiltering = mergedFiltering;
    }

    /**
     * Returns the merged indexing joiner,
     * reordered equal-first so the indexer chain always has its (merged) equal level at the top.
     * Computed on read to also cover {@link #addJoiner} appends.
     */
    public DefaultQuadJoiner<A, B, C, D> getMergedJoiner() {
        return mergedJoiner.reorderedEqualsFirst();
    }

    public @Nullable QuadPredicate<A, B, C, D> getMergedFiltering() {
        return mergedFiltering;
    }

    public void addJoiner(DefaultQuadJoiner<A, B, C, D> extraJoiner) {
        mergedJoiner = mergedJoiner.and(extraJoiner);
    }

}
