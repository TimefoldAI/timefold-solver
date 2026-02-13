package ai.timefold.solver.core.impl.domain.valuerange;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;
import ai.timefold.solver.core.impl.domain.valuerange.sort.SortableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.sort.ValueRangeSorter;

import org.jspecify.annotations.NullMarked;

/**
 * Abstract superclass for {@link ValueRange}.
 *
 * @see ValueRange
 * @see ValueRangeFactory
 */
@NullMarked
public abstract sealed class AbstractValueRange<T> implements ValueRange<T>, SortableValueRange<T>
        permits BigDecimalValueRange, BigIntegerValueRange, BooleanValueRange, CompositeValueRange, EmptyValueRange,
        IntValueRange, ListValueRange, LongValueRange, NullAllowingValueRange, SetValueRange, TemporalValueRange {

    @Override
    public boolean isEmpty() {
        return getSize() == 0L;
    }

    @Override
    public ValueRange<T> sort(ValueRangeSorter<T> sorter) {
        // The sorting operation is not supported by default
        // and must be explicitly implemented by the child classes if needed.
        throw new UnsupportedOperationException();
    }
}
