package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.joiner;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.function.BiEnumeratingFilter;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.function.BiEnumeratingJoiner;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record FilteringBiEnumeratingJoiner<Solution_, A, B>(
        BiEnumeratingFilter<Solution_, A, B> filter) implements BiEnumeratingJoiner<A, B> {

    @Override
    public FilteringBiEnumeratingJoiner<Solution_, A, B> and(BiEnumeratingJoiner<A, B> otherJoiner) {
        FilteringBiEnumeratingJoiner<Solution_, A, B> castJoiner = (FilteringBiEnumeratingJoiner<Solution_, A, B>) otherJoiner;
        return new FilteringBiEnumeratingJoiner<>(filter.and(castJoiner.filter()));
    }

}
