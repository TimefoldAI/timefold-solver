package ai.timefold.solver.core.impl.move.streams.dataset.joiner;

import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataFilter;
import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataJoiner;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record FilteringBiDataJoiner<Solution_, A, B>(BiDataFilter<Solution_, A, B> filter) implements BiDataJoiner<A, B> {

    @Override
    public FilteringBiDataJoiner<Solution_, A, B> and(BiDataJoiner<A, B> otherJoiner) {
        FilteringBiDataJoiner<Solution_, A, B> castJoiner = (FilteringBiDataJoiner<Solution_, A, B>) otherJoiner;
        return new FilteringBiDataJoiner<>(filter.and(castJoiner.filter()));
    }

}
