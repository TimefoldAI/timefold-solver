package ai.timefold.solver.core.impl.move.streams.pickers;

import java.util.Objects;
import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.pickers.BiPicker;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record FilteringBiPicker<A, B>(BiPredicate<A, B> filter)
        implements
            BiPicker<A, B> {

    @Override
    public FilteringBiPicker<A, B> and(BiPicker<A, B> otherPicker) {
        var castJoiner = (FilteringBiPicker<A, B>) otherPicker;
        return new FilteringBiPicker<>(filter.and(castJoiner.filter()));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FilteringBiPicker<?, ?> other
                && Objects.equals(filter, other.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(filter);
    }

}
