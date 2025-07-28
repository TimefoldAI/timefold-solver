package ai.timefold.solver.core.impl.move.streams.dataset.joiner;

import java.util.Objects;

import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataFilter;
import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataJoiner;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class FilteringBiDataJoiner<Solution_, A, B> implements BiDataJoiner<A, B> {

    private final BiDataFilter<Solution_, A, B> filter;

    public FilteringBiDataJoiner(BiDataFilter<Solution_, A, B> filter) {
        this.filter = filter;
    }

    @Override
    public FilteringBiDataJoiner<Solution_, A, B> and(BiDataJoiner<A, B> otherJoiner) {
        FilteringBiDataJoiner<Solution_, A, B> castJoiner = (FilteringBiDataJoiner<Solution_, A, B>) otherJoiner;
        return new FilteringBiDataJoiner<>(filter.and(castJoiner.getFilter()));
    }

    public BiDataFilter<Solution_, A, B> getFilter() {
        return filter;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FilteringBiDataJoiner<?, ?, ?> other
                && Objects.equals(filter, other.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filter);
    }
}
