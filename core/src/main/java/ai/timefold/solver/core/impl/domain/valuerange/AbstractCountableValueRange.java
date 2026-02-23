package ai.timefold.solver.core.impl.domain.valuerange;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;
import ai.timefold.solver.core.impl.domain.valuerange.sort.SortableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.sort.ValueRangeSorter;

import org.jspecify.annotations.NullMarked;

/**
 * Abstract superclass for {@link CountableValueRange} (and therefore {@link ValueRange}).
 *
 * @see CountableValueRange
 * @see ValueRange
 * @see ValueRangeFactory
 */
@NullMarked
public abstract class AbstractCountableValueRange<T> implements CountableValueRange<T>, SortableValueRange<T> {

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
